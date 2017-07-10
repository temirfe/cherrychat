package kg.prosoft.chatqal.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.ColorFilter;
import android.graphics.LightingColorFilter;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import kg.prosoft.chatqal.R;
import kg.prosoft.chatqal.model.Message;

public class ReceiverThreadAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private static String TAG = ReceiverThreadAdapter.class.getSimpleName();

    private String userId;
    private int SELF = 100;

    private Context mContext;
    private ArrayList<Message> messageArrayList;
    public static ArrayList<String> dateList;
    public static Map<String,String> dmap;
    public static SimpleDateFormat sdf;
    public static SimpleDateFormat sdf2;

    public static Date dateInst;
    public static String dateTodayString;

    public class ViewHolder extends RecyclerView.ViewHolder {
        TextView message, timestamp, tv_date;
        ImageView iv_clock, iv_sent, iv_second;

        public ViewHolder(View view) {
            super(view);
            message = (TextView) view.findViewById(R.id.message);
            timestamp = (TextView) view.findViewById(R.id.timestamp);
            tv_date = (TextView) view.findViewById(R.id.mydate);
            iv_clock = (ImageView) view.findViewById(R.id.icon_clock);
            iv_sent = (ImageView) view.findViewById(R.id.icon_sent);
            iv_second = (ImageView) view.findViewById(R.id.icon_second);
        }
    }


    public ReceiverThreadAdapter(Context mContext, ArrayList<Message> messageArrayList, String userId) {
        this.mContext = mContext;
        this.messageArrayList = messageArrayList;
        this.userId = userId;

        sdf = new SimpleDateFormat("yyyy-MM-dd");
        sdf2 = new SimpleDateFormat("MMM d, yyyy");
        dateInst = Calendar.getInstance().getTime();
        dateTodayString=sdf.format(dateInst);

        dateList=new ArrayList<>();
        dmap=new HashMap<>();
    }

    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView;

        // view type is to identify where to render the chat message
        // left or right
        if (viewType == SELF) {
            // self message
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_self, parent, false);
        } else {
            // others message
            itemView = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.chat_item_other, parent, false);
        }


        return new ViewHolder(itemView);
    }


    @Override
    public int getItemViewType(int position) {
        Message message = messageArrayList.get(position);
        /*if (message.getUser().getId().equals(userId)) {
            return SELF;
        }*/
        if (message.getSenderId().equals(userId)) {
            return SELF;
        }

        return position;
    }

    @Override
    public void onBindViewHolder(final RecyclerView.ViewHolder holder, int position) {
        Message message = messageArrayList.get(position);
        ((ViewHolder) holder).message.setText(message.getMessage());

        String createdAt=message.getCreatedAt();
        String timestamp = getTimeStamp(createdAt);
        String mId=message.getId();
        int mStatus=message.getStatus();
        getMyDate(createdAt,mId);

        if(message.getSenderId().equals(userId)){
            showStatusIcon((ViewHolder) holder, mStatus);
        }

        if(!dmap.isEmpty()){
            String myDate=dmap.get(mId);
            if(myDate!=null && !myDate.isEmpty()){
                ((ViewHolder) holder).tv_date.setVisibility(View.VISIBLE);
                ((ViewHolder) holder).tv_date.setText(myDate);
                //Log.e(TAG, "id:"+mId+", date: "+myDate+", msg:"+ message.getMessage());
            }
            else{
                ((ViewHolder) holder).tv_date.setVisibility(View.GONE);
            }
        }

        /*if (message.getUser().getName() != null)
            timestamp = message.getUser().getName() + ", " + timestamp;*/

        ((ViewHolder) holder).timestamp.setText(timestamp);
    }

    @Override
    public int getItemCount() {
        return messageArrayList.size();
    }

    public static String getTimeStamp(String dateStr) {
        SimpleDateFormat sdformat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        String timestamp = "";

        try {
            Date date = sdformat.parse(dateStr);
            SimpleDateFormat todayFormat = new SimpleDateFormat("dd");
            String dateToday = todayFormat.format(date);
            sdformat = new SimpleDateFormat("H:mm");
            String date1 = sdformat.format(date);
            timestamp = date1.toString();
        } catch (ParseException e) {
            e.printStackTrace();
        }

        return timestamp;
    }

    public static void getMyDate(String dateStr, String msg_id) {
        String myDate;
        try {
            Date dateChat = sdf.parse(dateStr);
            String dateChatString=sdf.format(dateChat);
            if(!dateList.contains(dateChatString)){
                dateList.add(dateChatString);
                if(dateTodayString.equals(dateChatString)){
                    myDate="Today";
                }
                else{
                    myDate = sdf2.format(dateChat);
                }
                dmap.put(msg_id,myDate);
            }

        } catch (ParseException e) {
            e.printStackTrace();
        }
    }

    public void showStatusIcon(ViewHolder holder, int mStatus){
        holder.iv_clock.setVisibility(View.GONE);
        holder.iv_sent.setVisibility(View.GONE);
        holder.iv_second.setVisibility(View.GONE);

        Drawable myIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_check_black_24dp);
        ColorFilter filter = new LightingColorFilter( Color.BLACK, Color.BLACK);
        myIcon.setColorFilter(filter);
        holder.iv_sent.setImageDrawable(myIcon);
        holder.iv_second.setImageDrawable(myIcon);

        if(mStatus==0) //sending
        {
            holder.iv_clock.setVisibility(View.VISIBLE);
        }
        else if(mStatus==1) //sent
        {
            holder.iv_sent.setVisibility(View.VISIBLE);
        }
        else if(mStatus==2) //delivered
        {
            holder.iv_sent.setVisibility(View.VISIBLE);
            holder.iv_second.setVisibility(View.VISIBLE);
        }
        else if(mStatus==3) //read
        {
            myIcon = ContextCompat.getDrawable(mContext, R.drawable.ic_check_black_24dp);
            filter = new LightingColorFilter( Color.BLUE, Color.BLUE );
            myIcon.setColorFilter(filter);
            holder.iv_sent.setImageDrawable(myIcon);
            holder.iv_second.setImageDrawable(myIcon);
            holder.iv_sent.setVisibility(View.VISIBLE);
            holder.iv_second.setVisibility(View.VISIBLE);
            //holder.iv_sent.setAlpha(1);
            //holder.iv_second.setAlpha(1);
        }
    }
}

