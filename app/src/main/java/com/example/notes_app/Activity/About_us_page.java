package com.example.notes_app.Activity;

import static android.content.ContentValues.TAG;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.os.Bundle;
import android.util.Log;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import mehdi.sakout.aboutpage.AboutPage;
import mehdi.sakout.aboutpage.Element;

import com.example.notes_app.R;

public class About_us_page extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Element versionElement = new Element();
        versionElement.setTitle("Version 2.0");

        Element adsElement = new Element();
        adsElement.setTitle("Advertise with us");

        View aboutPage = new AboutPage(this)
                .isRTL(false)
                .setImage(R.drawable.app_logo) // Replace with your app logo
                .setDescription("Cloudify helps you take quick notes, organize tasks, and stay productive! It is an open-source project, and you can explore its code, contribute, or report issues on GitHub.")
                .addItem(versionElement)
                .addItem(adsElement)
                .addGroup("Connect with us")
                .addEmail("support@yourapp.com")
                .addWebsite("https://smitcloud.org")
//                .addFacebook("your_facebook_page")
//                .addTwitter("your_twitter_handle")
                .addYoutube("your_youtube_channel_id")
//                .addPlayStore("com.yourapp.package")
                .addGitHub("Smitvirja/note_cloudy")
                .addInstagram("smit24x7")
                .create();

        setContentView(aboutPage);
    }
}
