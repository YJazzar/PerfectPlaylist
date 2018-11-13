package com.example.faari.perfectplaylsit;

import android.Manifest;
import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.OnLifecycleEvent;
import android.arch.persistence.room.Room;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.ListView;

import com.hound.android.fd.DefaultRequestInfoFactory;
import com.hound.android.fd.Houndify;
import com.hound.android.fd.UserIdFactory;
import com.hound.android.sdk.VoiceSearch;
import com.hound.android.sdk.VoiceSearchInfo;
import com.hound.android.sdk.audio.SimpleAudioByteStreamSource;
import com.hound.android.sdk.util.HoundRequestInfoFactory;
import com.hound.core.model.sdk.HoundRequestInfo;
import com.hound.core.model.sdk.PartialTranscript;
import com.spotify.android.appremote.api.ConnectionParams;
import com.spotify.android.appremote.api.Connector;
import com.spotify.android.appremote.api.SpotifyAppRemote;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.lang.reflect.Array;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class MainActivity extends AppCompatActivity {

    private static final String CLIENT_ID = "ffe427ae0c784377ab9f5afc7bf47a15";
    private static final String REDIRECT_URI = "https://example.com/redirect/";
    final static int REQUEST_CODE = 5744;
    private SpotifyAppRemote mSpotifyAppRemote;
    private VoiceSearch mvoiceSearch;
    private FloatingActionButton mbuttonSearch;
    ViewPager mviewPager;
    SectionsPagerAdapter msectionsPagerAdapter;
    SongDatabase songDB;
    CommandDatabase commandDB;
    SongAdapter songAdapter;
    ArrayAdapter<Command> commandAdapter;
    CurrentState state;


    public static void SpotifyWebAPIParser(String rawResponse) {
        try {
            JSONObject raw = new JSONObject(rawResponse);
            JSONArray tracksObj = raw.getJSONArray("tracks");
            ArrayList<Song> songs = new ArrayList<>();

            String name;
            String uri;
            String id;
            String imgUrl;
            String album;
            String artists;
            int duration;
            String[] arr = new String[6];
            JSONObject temp = null;
            for (int i = 0; i < tracksObj.length(); i ++) {
                temp = tracksObj.getJSONObject(i);

                arr[0] = temp.getString("name");
                arr[2] = temp.getString("uri");
                arr[3] = temp.getString("id");
                arr[4] = temp.getJSONObject("album").getJSONArray("images").getJSONObject( temp.getJSONObject("album").getJSONArray("images").length()-1).getString("url");
                arr[5] = temp.getJSONObject("album").getString("name");
                arr[6] = temp.getJSONArray("artists").getJSONObject(0).getString("name");
                duration = Integer.parseInt(temp.getString("duration_ms"));

                songs.add(new Song(arr, duration));

            }



        } catch (Exception e) {

        }

    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        state = (CurrentState) getApplication();
        if(state.getCommandList() == null) state.setCommandList(new ArrayList<Command>());
        if(state.getSongList() == null) state.setSongList(new ArrayList<Song>());
        mbuttonSearch = findViewById(R.id.fab_microphone);
        msectionsPagerAdapter = new SectionsPagerAdapter(getSupportFragmentManager());
        mviewPager = findViewById(R.id.container);
        mviewPager.setAdapter(msectionsPagerAdapter);
        songAdapter = new SongAdapter(getApplicationContext(), state.getSongList());
        commandAdapter = new ArrayAdapter<Command>(getApplicationContext(), android.R.layout.simple_list_item_1, state.getCommandList());

        ActivityCompat.requestPermissions(this, new String[]{
                Manifest.permission.RECORD_AUDIO,
                Manifest.permission.WRITE_EXTERNAL_STORAGE,
        }, 0);

        final Houndify houndify = Houndify.get(this);
        houndify.setClientId("n06WnSgzJbML7AuGNJou3Q==");
        houndify.setClientKey("ZzWH-lZ41uFCHq75opj9T5Zykux3aAWdDWLCCL8mPPzGR51Erds4gvnLT5v-TBzDs-qH9CoHNpdEG-oyDwVbmw==");
        houndify.setRequestInfoFactory(new DefaultRequestInfoFactory(this));
    }

    @Override
    protected void onStart(){
        super.onStart();

        mbuttonSearch.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Houndify.get(MainActivity.this).voiceSearch(MainActivity.this, REQUEST_CODE);
                Command command = new Command("Some new command yay!");
                PlaceholderFragment.getListViewPlaylist().setAdapter(songAdapter);
                PlaceholderFragment.getListViewCommands().setAdapter(commandAdapter);
                state.getCommandList().add(0, command);
                commandAdapter.notifyDataSetChanged();
                for(int i = 0; i<6; i++) {
                    state.getSongList().add(new Song("gregreg"));
                }
                songAdapter.notifyDataSetChanged();
                Intent intent1 = new Intent(getApplicationContext(), UpdateDatabaseService.class);
                startService(intent1);
                mviewPager.setCurrentItem(2);
            }
        });

        ConnectionParams connectionParams = new ConnectionParams.Builder(CLIENT_ID)
                .setRedirectUri(REDIRECT_URI)
                .showAuthView(true)
                .build();

        SpotifyAppRemote.CONNECTOR.connect(this, connectionParams, new Connector.ConnectionListener() {
            @Override
            public void onConnected(SpotifyAppRemote spotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote;
                Log.d("MainActivity", "Connected! Yay!");


                connected();
            }

            @Override
            public void onFailure(Throwable throwable) {
                Log.e("MainActivity", throwable.getMessage(), throwable);

            }
        });
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        //if (requestCode == REQUEST_CODE) {
        //final HoundSearchResult result = Houndify.get(this).fromActivityResult(resultCode, data);
        //final HoundResponse houndResponse = result.getResponse();
        //}
    }

    private void buildVoiceSearch() {
        if (mvoiceSearch != null) {
            return; // We are already searching
        }

        mvoiceSearch = new VoiceSearch.Builder()
                .setRequestInfo(buildRequestInfo())
                .setClientId("n06WnSgzJbML7AuGNJou3Q==")
                .setClientKey("ZzWH-lZ41uFCHq75opj9T5Zykux3aAWdDWLCCL8mPPzGR51Erds4gvnLT5v-TBzDs-qH9CoHNpdEG-oyDwVbmw==")
                .setListener(voiceListener)
                .setAudioSource(new SimpleAudioByteStreamSource())
                .build();

        //Houndify.get(this).mvoiceSearch(this, REQUEST_CODE);
        mvoiceSearch.start();
    }

    private HoundRequestInfo buildRequestInfo() {
        final HoundRequestInfo requestInfo = HoundRequestInfoFactory.getDefault(this);
        requestInfo.setUserId(UserIdFactory.get(this));
        requestInfo.setRequestId(UUID.randomUUID().toString());
        return requestInfo;
    }

    private void connected(){
        mSpotifyAppRemote.getPlayerApi().play("spotify:user:spotify:playlist:37i9dQZF1DX2sUQwD7tbmL");
    }

    public void pauseMusic(View view){
        mSpotifyAppRemote.getPlayerApi().pause();
    }

    public void skipMusic(View view){
        mSpotifyAppRemote.getPlayerApi().skipNext();
    }

    public void playbackMusic(View view){
        mSpotifyAppRemote.getPlayerApi().skipPrevious();
    }

    public void startVoiceSearch(View view){
        if (mvoiceSearch == null) {
            buildVoiceSearch();
        }
        else {
            mvoiceSearch.stopRecording();
        }
    }

    @Override
    protected void onStop(){
        super.onStop();

        SpotifyAppRemote.CONNECTOR.disconnect(mSpotifyAppRemote);
    }

    public static class PlaceholderFragment extends Fragment {
        /**
         * The fragment argument representing the section number for this
         * fragment.
         */
        private static final String ARG_SECTION_NUMBER = "section_number";
        private static View recentView, playlistView;

        public PlaceholderFragment() {}

        /**
         * Returns a new instance of this fragment for the given section
         * number.
         */
        public static PlaceholderFragment newInstance(int sectionNumber) {
            PlaceholderFragment fragment = new PlaceholderFragment();
            Bundle args = new Bundle();
            args.putInt(ARG_SECTION_NUMBER, sectionNumber);
            fragment.setArguments(args);
            return fragment;
        }

        @Override
        public View onCreateView(LayoutInflater inflater, ViewGroup container,
                                 Bundle savedInstanceState) {
            recentView = inflater.inflate(R.layout.fragment_recent, container, false);
            playlistView = inflater.inflate(R.layout.fragment_playlist, container, false);
            if(getArguments().getInt(ARG_SECTION_NUMBER)==1){
                return recentView;
            } else if(getArguments().getInt(ARG_SECTION_NUMBER)==2){
                return playlistView;
            }
            return recentView;
        }

        public static ListView getListViewCommands(){
            return recentView.findViewById(R.id.lv_commands);
        }

        public static ListView getListViewPlaylist(){
            return playlistView.findViewById(R.id.lv_playlist);
        }
    }

    private final Listener voiceListener = new Listener();

    //------------------------------------------------------------------------------------------------------------
    private class Listener implements VoiceSearch.RawResponseListener {

        @Override
        public void onTranscriptionUpdate(final PartialTranscript transcript) {
            switch (mvoiceSearch.getState()) {
                case STATE_STARTED:
                    //statusTextView.setText("Listening...");
                    break;

                case STATE_SEARCHING:
                    //statusTextView.setText("Receiving...");
                    break;

                default:
                    //statusTextView.setText("Unknown");
                    break;
            }

            //contentTextView.setText("Transcription:\n" + transcript.getPartialTranscript());
        }

        @Override
        public void onResponse(String rawResponse, VoiceSearchInfo voiceSearchInfo) {
            mbuttonSearch.setClickable(true);
            mvoiceSearch = null;

            //statusTextView.setText("Received Response");

            ArrayList<String> seeds = new ArrayList<>();
            String jsonString;
            try {
                JSONObject jsonObj = new JSONObject(rawResponse);
                JSONArray results = jsonObj.getJSONArray("AllResults");
                for (int k = 0; k < results.length(); k++) {
                    JSONObject nativeData = results.getJSONObject(k).getJSONObject("NativeData");
                    JSONObject track1 = nativeData.getJSONArray("Tracks").getJSONObject(0);
                    JSONArray thirdParty = track1.getJSONArray("MusicThirdPartyIds");

                    int index = -1;
                    for (int i = 0; i < thirdParty.length(); i++) {

                        String name = thirdParty.getJSONObject(i).getJSONObject("MusicThirdParty").getString("Name");
                        if (name.equals("Spotify")) {
                            index = i;
                        }
                    }

                    String seed = thirdParty.getJSONObject(index).getJSONArray("Ids").toString();


                    String Resultingtrack = "The program returned the song: " + track1.getString("TrackName") + " - by:  " + track1.getString("ArtistName")
                            + " with the spotify ID: " + seed + " and extracted is: " + seed.substring(16, seed.length() - 2);
                    Log.d("PROGRAM-RESULT", Resultingtrack);
                    seeds.add(seed.substring(16, seed.length() - 2));
                }
                String output = "";
                for (String s : seeds)
                {
                    output += s + "\t";
                }
                Log.d("PROGRAM-RESULT", output);
            } catch (JSONException ex){
                jsonString = "failed";
                Log.d("PROGRAM-RESULT", jsonString);
            }

        }

        @Override
        public void onError(final Exception ex, final VoiceSearchInfo info) {
            mvoiceSearch = null;

            //statusTextView.setText("Something went wrong");
            //contentTextView.setText(ex.toString());
        }

        @Override
        public void onRecordingStopped() {
            //statusTextView.setText("Receiving...");
        }

        @Override
        public void onAbort(final VoiceSearchInfo info) {
            mvoiceSearch = null;
            //statusTextView.setText("Aborted");
        }
    }

    /**
     * A {@link FragmentPagerAdapter} that returns a fragment corresponding to
     * one of the sections/tabs/pages.
     */
    public class SectionsPagerAdapter extends FragmentPagerAdapter {

        private SectionsPagerAdapter(FragmentManager fm) {
            super(fm);
        }

        @Override
        public Fragment getItem(int position) {
            // getItem is called to instantiate the fragment for the given page.
            // Return a PlaceholderFragment (defined as a static inner class below).
            return PlaceholderFragment.newInstance(position + 1);
        }

        @Override
        public int getCount() {
            // Show 2 total pages.
            return 2;
        }
    }
}