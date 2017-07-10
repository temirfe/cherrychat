/**
 * Copyright 2016 Google Inc. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package kg.prosoft.chatqal.service;

import android.content.Context;
import android.content.Intent;
import android.support.v4.content.LocalBroadcastManager;
import android.text.TextUtils;
import android.util.Log;

import com.google.firebase.messaging.FirebaseMessagingService;
import com.google.firebase.messaging.RemoteMessage;

import org.json.JSONException;
import org.json.JSONObject;

import kg.prosoft.chatqal.MainActivity;
import kg.prosoft.chatqal.app.Config;
import kg.prosoft.chatqal.model.Message;
import kg.prosoft.chatqal.model.User;
import kg.prosoft.chatqal.utils.NotificationUtils;

public class MyFirebaseMessagingService extends FirebaseMessagingService {

      private static final String TAG = MyFirebaseMessagingService.class.getSimpleName();

      private NotificationUtils notificationUtils;

      @Override
      public void onMessageReceived(RemoteMessage remoteMessage) {
        Log.e(TAG, "From: " + remoteMessage.getFrom());

        if (remoteMessage == null)
          return;

        // Check if message contains a notification payload.
        if (remoteMessage.getNotification() != null) {
          Log.e(TAG, "Notification Body: " + remoteMessage.getNotification().getBody());
          handleNotification(remoteMessage.getNotification().getBody());
        }

        // Check if message contains a data payload.
        if (remoteMessage.getData().size() > 0) {
          Log.e(TAG, "Data Payload: " + remoteMessage.getData().toString());

          try {
            JSONObject json = new JSONObject(remoteMessage.getData().toString());
            if(json.has("data")){
              handleDataMessage(json);
            }
            else if(json.has("status"))
            {
              handleStatusMessage(json);
            }
          } catch (Exception e) {
            Log.e(TAG, "Exception: " + e.getMessage());
          }
        }
      }

      private void handleNotification(String message) {
        if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
          // app is in foreground, broadcast the push message
          Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);
          pushNotification.putExtra("message", message);
          LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

          // play notification sound
          NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());
          notificationUtils.playNotificationSound();

          Log.e(TAG, "handleNotif: " + message);
        }else{
          // If the app is in background, firebase itself handles the notification
          Log.e(TAG, "Notification is handled by Firebase nigga");
        }
    }

      private void handleDataMessage(JSONObject json) {
        Log.e(TAG, "push json: " + json.toString());

        try {
          JSONObject data = json.getJSONObject("data");

          String title = data.getString("title");
          String message = data.getString("message");
          boolean isBackground = data.getBoolean("is_background");
          String imageUrl = data.getString("image");
          String timestamp = data.getString("timestamp");
          int type = data.getInt("type");
          String to_user_id = data.getString("to_user_id");
          String from_user_id = data.getString("from_user_id");
          JSONObject payload = data.getJSONObject("payload");

          /*Log.e(TAG, "title: " + title);
          Log.e(TAG, "message: " + message);
          Log.e(TAG, "isBackground: " + isBackground);
          Log.e(TAG, "payload: " + payload.toString());
          Log.e(TAG, "imageUrl: " + imageUrl);
          Log.e(TAG, "timestamp: " + timestamp);
          Log.e(TAG, "type: " + type);
          Log.e(TAG, "to_user_id: " + to_user_id);*/

            //chat
            JSONObject mObj = data.getJSONObject("messageObject");
            Message chatMessage = new Message();
            chatMessage.setMessage(mObj.getString("message"));
            chatMessage.setId(mObj.getString("message_id"));
            chatMessage.setCreatedAt(mObj.getString("created_at"));
            chatMessage.setReceiverId(to_user_id);
            chatMessage.setSenderId(from_user_id);
            chatMessage.setUniqueId(mObj.getString("unique_id"));


          if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {
            // app is in foreground, broadcast the push message
            Intent pushNotification = new Intent(Config.PUSH_NOTIFICATION);
            pushNotification.putExtra("message", chatMessage);
            pushNotification.putExtra("type", type);
            pushNotification.putExtra("to_user_id", to_user_id);
            pushNotification.putExtra("from_user_id", from_user_id);
            LocalBroadcastManager.getInstance(this).sendBroadcast(pushNotification);

            /*Log.e(TAG, "138 playsound triggered");
            // play notification sound
            NotificationUtils notificationUtils = new NotificationUtils(getApplicationContext());
            notificationUtils.playNotificationSound();*/
          } else {
            // app is in background, show the notification in notification tray
            Intent resultIntent = new Intent(getApplicationContext(), MainActivity.class);
            resultIntent.putExtra("message", message);

            // check for image attachment
            if (TextUtils.isEmpty(imageUrl)) {
              showNotificationMessage(getApplicationContext(), title, message, timestamp, resultIntent);
            } else {
              // image is present, show notification with image
              showNotificationMessageWithBigImage(getApplicationContext(), title, message, timestamp, resultIntent, imageUrl);
            }
          }
        } catch (JSONException e) {
          Log.e(TAG, "Json Exception: " + e.getMessage());
        } catch (Exception e) {
          Log.e(TAG, "Exception: " + e.getMessage());
        }
      }

      private void handleStatusMessage(JSONObject json) {
        Log.e(TAG, "status push json: " + json.toString());

        try {
          JSONObject data = json.getJSONObject("status");

          String status = data.getString("status");
          String message_id = data.getString("message_id");
          String unique_id = data.getString("unique_id");

          Log.e(TAG, "status: " + status);
          Log.e(TAG, "message_id: " + message_id);
          Log.e(TAG, "unique_id: " + unique_id);


          if (!NotificationUtils.isAppIsInBackground(getApplicationContext())) {

              //Log.e(TAG, "statusNotification sent to broadcast");
            // app is in foreground, broadcast the push message
              Intent statusNotification = new Intent(Config.STATUS_NOTIFICATION);
              statusNotification.putExtra("status", status);
              //statusNotification.putExtra("message_id", message_id);
              statusNotification.putExtra("unique_id", unique_id);
              LocalBroadcastManager.getInstance(this).sendBroadcast(statusNotification);
          } else {
            // app is in background, show the notification in notification tray
          }
        } catch (JSONException e) {
          Log.e(TAG, "Json Exception: " + e.getMessage());
        } catch (Exception e) {
          Log.e(TAG, "Exception: " + e.getMessage());
        }
      }

      /**
       * Showing notification with text only
       */
      private void showNotificationMessage(Context context, String title, String message, String timeStamp, Intent intent) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent);
      }

      /**
       * Showing notification with text and image
       */
      private void showNotificationMessageWithBigImage(Context context, String title, String message, String timeStamp, Intent intent, String imageUrl) {
        notificationUtils = new NotificationUtils(context);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        notificationUtils.showNotificationMessage(title, message, timeStamp, intent, imageUrl);
      }


  }