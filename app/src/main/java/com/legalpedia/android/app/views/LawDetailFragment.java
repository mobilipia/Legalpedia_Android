package com.legalpedia.android.app.views;

import android.app.ProgressDialog;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v4.widget.SwipeRefreshLayout;
import android.text.Html;
import android.text.SpannableString;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.BackgroundColorSpan;
import android.text.style.ForegroundColorSpan;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.legalpedia.android.app.AddNoteActivity;
import com.legalpedia.android.app.App;
import com.legalpedia.android.app.LawDetailViewActivity;
import com.legalpedia.android.app.R;
import com.legalpedia.android.app.adapter.NoteListAdapter;
import com.legalpedia.android.app.models.Annotations;
import com.legalpedia.android.app.models.AnnotationsDao;
import com.legalpedia.android.app.models.DaoSession;
import com.legalpedia.android.app.models.Laws;
import com.legalpedia.android.app.models.LawsDao;
import com.legalpedia.android.app.models.Sections;
import com.legalpedia.android.app.models.SectionsDao;
import com.legalpedia.android.app.util.LGPClient;

import org.greenrobot.greendao.query.QueryBuilder;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by adebayoolabode on 4/9/17.
 */

public class LawDetailFragment  extends Fragment {

    private NoteListAdapter adapter;
    private String sectionid;
    private TextView titleView;
    private TextView bodyView;
    private View rootView;
    private SwipeRefreshLayout mSwipeRefreshLayout;
    private ProgressDialog progressDialog;
    private SharedPreferences sharedpreferences = null;
    private Context ctx=null;
    private int offset=0;
    private int limit=20;
    private int page=0;
    private int uid=0;
    private int resource=0;
    private boolean loading = true;
    private int pastVisiblesItems, visibleItemCount, totalItemCount;
    public String lawid;
    private String searchtext="";
    private List<String> highlightlist=new ArrayList<String>();
    private DaoSession daoSession;
    private static final int TRANSLATE = 1;
    private String title="";
    private AnnotationsDao annotationsDao;
    private String LOGINPREFERENCES="login_data";

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        rootView = inflater.inflate(R.layout.lawdetail_fragment, container, false);
        ctx=rootView.getContext();
        progressDialog = new ProgressDialog(ctx,R.style.customDialog);
        resource =3;
        sharedpreferences=ctx.getSharedPreferences(LOGINPREFERENCES, Context.MODE_PRIVATE);
        uid = Integer.parseInt(sharedpreferences.getString("uid","0"));
        LawDetailViewActivity activity = (LawDetailViewActivity)ctx;
        Intent intent=activity.getIntent();
        sectionid=intent.getStringExtra("sectionid");
        daoSession = ((App)  activity.getApplication()).getDaoSession();
        annotationsDao = daoSession.getAnnotationsDao();
        Log.d("Law ID ",sectionid);
        try {
            if(intent.getStringExtra("searchtext")!=null){
                searchtext =  intent.getStringExtra("searchtext");
                highlightlist.add(searchtext);
            }
        }
        catch(Exception ex){
            ex.printStackTrace();
        }

        daoSession = ((App)  activity.getApplication()).getDaoSession();
        bodyView = (TextView) rootView.findViewById(R.id.body);

        if(Build.VERSION.SDK_INT>=11){
            Log.d("JudgementFragment","Enabled annotation");
            bodyView.setTextIsSelectable(true);
            bodyView.setCustomSelectionActionModeCallback(new ActionMode.Callback() {
                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    menu.add(0, TRANSLATE, 0, "Translate").setIcon(R.drawable.ic_discuss); //choose any icon
                    // Remove the other options
                    //menu.removeItem(android.R.id.selectAll);
                    //menu.removeItem(android.R.id.cut);
                    //menu.removeItem(android.R.id.copy);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return true;
                }

                @Override
                public boolean onActionItemClicked(ActionMode mode, MenuItem item) {
                    switch (item.getItemId()) {
                        case TRANSLATE:
                            int min = 0;
                            int max = bodyView.getText().length();
                            if (bodyView.isFocused()) {
                                final int selStart = bodyView.getSelectionStart();
                                final int selEnd = bodyView.getSelectionEnd();

                                min = Math.max(0, Math.min(selStart, selEnd));
                                max = Math.max(0, Math.max(selStart, selEnd));
                            }

                            final CharSequence selectedText = bodyView.getText().subSequence(min, max); //this is your desired string
                            //Toast.makeText(getActivity().getApplicationContext(), selectedText, Toast.LENGTH_SHORT).show();
                            //ctx.getSystemService(ctx.CLIPBOARD_SERVICE).setText(selectedText);
                            //Here put your code for translation
                            String annotationtext= selectedText.toString();
                            String comment="";
                            String titleshare="Share using";
                            highlightString(annotationtext);
                            String sharedtext=title+"\n\n"+annotationtext+"\n\n Sent from Legalpedia Android https://www.legalpediaonline.com/";
                            shareToSocial(sharedtext,titleshare);
                            saveToDB(title,annotationtext,comment);

                            mode.finish();
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {

                }
            });


        }
        Typeface face=Typeface.createFromAsset(getActivity().getAssets(),
                "Verdana.ttf");

        bodyView.setTypeface(face);
        new UpdateLaws().execute();
        return rootView;
    }



    public void shareToSocial(String message,String title){
        Intent share = new Intent(Intent.ACTION_SEND);
        share.setType("text/plain");
        share.putExtra(Intent.EXTRA_SUBJECT, "Legalpedia");
        share.putExtra(Intent.EXTRA_TEXT, message);

        startActivity(Intent.createChooser(share, title));
    }

    public void saveToDB(String title,String content,String comment){
        Annotations annotations = new Annotations();
        annotations.setTitle(title);
        annotations.setContent(content);
        annotations.setResource(resource);
        annotations.setResourceid(Integer.parseInt(lawid));
        annotations.setUid(uid);
        annotations.setComment(comment);
        annotationsDao.save(annotations);
        try {
            new SaveAnnotation().execute(String.valueOf(uid),String.valueOf(resource),lawid,title,content,"", comment);
        }
        catch(Exception ex){
            ex.printStackTrace();
        }
    }



    private void highlightString(String selected) {
        //Get the text from text view and create a spannable string
        SpannableString spannableString = new SpannableString(bodyView.getText());

        //Get the previous spans and remove them
        BackgroundColorSpan[] backgroundSpans = spannableString.getSpans(0, spannableString.length(), BackgroundColorSpan.class);

        for (BackgroundColorSpan span: backgroundSpans) {
            spannableString.removeSpan(span);
        }

        //Search for all occurrences of the keyword in the string
        int indexOfKeyword = spannableString.toString().indexOf(selected);

        while (indexOfKeyword > 0) {
            //Create a background color span on the keyword
            spannableString.setSpan(new BackgroundColorSpan(Color.YELLOW), indexOfKeyword, indexOfKeyword + selected.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

            //Get the next index of the keyword
            indexOfKeyword = spannableString.toString().indexOf(selected, indexOfKeyword + selected.length());
        }

        //Set the final text on TextView
        bodyView.setText(spannableString);
    }


    private void highlightStringAll(List<String> selectedlist) {
        //Get the text from text view and create a spannable string
        SpannableString spannableString = new SpannableString(bodyView.getText());

        //Get the previous spans and remove them
        BackgroundColorSpan[] backgroundSpans = spannableString.getSpans(0, spannableString.length(), BackgroundColorSpan.class);

        for (BackgroundColorSpan span: backgroundSpans) {
            spannableString.removeSpan(span);
        }

        for(String selected : selectedlist) {
            //Search for all occurrences of the keyword in the string
            int indexOfKeyword = spannableString.toString().indexOf(selected);

            while (indexOfKeyword > 0) {
                //Create a background color span on the keyword
                spannableString.setSpan(new BackgroundColorSpan(Color.YELLOW), indexOfKeyword, indexOfKeyword + selected.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);

                //Get the next index of the keyword
                indexOfKeyword = spannableString.toString().indexOf(selected, indexOfKeyword + selected.length());
            }

        }
        //Set the final text on TextView
        bodyView.setText(spannableString);
    }



    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.notelist, menu);
        super.onCreateOptionsMenu(menu, inflater);
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();



        // user is in notifications fragment
        // and selected 'Mark all as Read'
        if (id == R.id.edit) {

            Intent intent=new Intent(ctx,AddNoteActivity.class);
            startActivity(intent);
        }


        return super.onOptionsItemSelected(item);
    }



    class UpdateLaws  extends AsyncTask<String, String, List> {

        private SpannableStringBuilder bodyString;

        @Override
        protected List doInBackground(String... offset) {
            SectionsDao sectionsDao =daoSession.getSectionsDao();
            QueryBuilder qb = sectionsDao.queryBuilder();
            List resp=qb.where(SectionsDao.Properties.Id.eq(sectionid)).list();


            return resp;
        }
        @Override
        protected void onPostExecute(List resp) {
            if(resp.size()>0) {
                SpannableStringBuilder stringBuilder = new SpannableStringBuilder ();
                Sections section = (Sections) resp.get(0);
                //titleView = (TextView)rootView.findViewById(R.id.title);
                getActivity().setTitle(section.getTitle());
                title= section.getTitle();
                String titleText = "\n\t\t\t"+section.getTitle()+"\n\n";
                SpannableString titleString = new SpannableString(titleText);
                titleString.setSpan(new StyleSpan(Typeface.BOLD), 0, titleText.length(), 0);
                titleString.setSpan(new ForegroundColorSpan(Color.RED), 0, titleText.length(), 0);
                titleString.setSpan(new RelativeSizeSpan(1.4f), 0, titleText.length(), 0);
                String replaceHtml = section.getBody()
                        .replaceAll("&lt;", "<")
                        .replaceAll("&gt;", ">");

                Spanned span = Html.fromHtml(replaceHtml);
                SpannableString bodyString = new SpannableString(span);
                bodyString.setSpan(new BackgroundColorSpan(0x000000), 0, span.length(), Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                stringBuilder.append(titleString);
                stringBuilder.append(bodyString);
                bodyView.setText(stringBuilder);
                if(highlightlist.size()>0){
                    highlightStringAll(highlightlist);
                }

            }

            progressDialog.dismiss();
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setTitle("Legalpedia");
            progressDialog.setMessage("Processing...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.show();
        }
    }



    class SaveAnnotation extends AsyncTask<String,Void,JSONObject> {


        private JSONObject jsonobj;
        private ProgressDialog progressDialog = new ProgressDialog(ctx,R.style.customDialog);
        @Override
        protected JSONObject doInBackground(String... params) {
            jsonobj = LGPClient.addAnnotation(params[0],params[1],params[2],params[3],params[4],params[5],params[6]);
            return jsonobj;
        }

        @Override
        protected void onPostExecute(JSONObject jsonobj) {
            try {
                progressDialog.dismiss();
                Log.d("SaveAnnotation",jsonobj.toString());
            } catch (Exception ex) {
                ex.printStackTrace();
            }
        }

        @Override
        protected void onPreExecute() {
            progressDialog.setTitle("Legalpedia");
            progressDialog.setMessage("Processing...");
            progressDialog.setProgressStyle(ProgressDialog.STYLE_SPINNER);
            progressDialog.setIndeterminate(false);
            progressDialog.show();
        }
    }
}

