package com.example.test2;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private ArrayList<AppItem> appItems;

    @NonNull
    @Override
    public AppAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppAdapter.ViewHolder holder, int position) {
        holder.onBind(appItems.get(position));
    }

    public void setAppItems(ArrayList<AppItem> list) {
        this.appItems = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return appItems == null ? 0 : appItems.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        ImageView AppIcon;
        TextView AppUseTime;
        TextView AppName;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            AppIcon = itemView.findViewById(R.id.app_icon);
            AppUseTime = itemView.findViewById(R.id.app_use_time);
            AppName = itemView.findViewById(R.id.app_name);
        }

        void onBind(AppItem item) {
            AppUseTime.setText(item.getAppTime());
            AppIcon.setImageDrawable(item.getAppIcon());
            AppName.setText(item.getAppName());
        }
    }
}
