package com.example.test2;

import android.animation.ValueAnimator;
import android.content.Context;
import android.util.SparseBooleanArray;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class AppAdapter extends RecyclerView.Adapter<AppAdapter.ViewHolder> {

    private ArrayList<AppItem> appItems;
    private Context mContext;
    private SparseBooleanArray selectedItems = new SparseBooleanArray();
    private int prePosition = -1;

    @NonNull
    @Override
    public AppAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        mContext = parent.getContext();
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.app_activity, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AppAdapter.ViewHolder holder, int position) {
        holder.onBind(appItems.get(position), position);

        // 시간 범위 텍스트 설정
        String timeRange = String.format("%02d시 ~ %02d시", position, (position + 1) % 24);
        holder.timeTextView.setText(timeRange);
    }

    public void setAppItems(ArrayList<AppItem> list) {
        this.appItems = list;
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return 24; // 24시간에 맞게 아이템 개수를 설정
    }

    public class ViewHolder extends RecyclerView.ViewHolder implements View.OnClickListener {

        ImageView AppIcon;
        TextView AppUseTime;
        TextView AppName;
        TextView timeTextView;
        LinearLayout useTime;
        private AppItem item;
        private int position;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);

            AppIcon = itemView.findViewById(R.id.app_icon);
            AppUseTime = itemView.findViewById(R.id.app_use_time);
            AppName = itemView.findViewById(R.id.app_name);
            useTime = itemView.findViewById(R.id.use_time);
            timeTextView = itemView.findViewById(R.id.time_textView);
        }

        void onBind(AppItem item, int position) {
            this.item = item;
            this.position = position;

            AppUseTime.setText(item.getAppTime());
            AppIcon.setImageDrawable(item.getAppIcon());
            AppName.setText(item.getAppName());

            changeVisibility(selectedItems.get(position));

            itemView.setOnClickListener(this);
        }

        @Override
        public void onClick(View view) {
//            if (selectedItems.get(position)) {
//                selectedItems.delete(position);
//            } else {
//                selectedItems.delete(prePosition);
//                selectedItems.put(position, true);
//            }
//
//            if (prePosition != -1) {
//                notifyItemChanged(prePosition);
//            }
//
//            notifyItemChanged(position);
//            prePosition = position;

            if (view.getId() == R.id.linearItem) {
                if (selectedItems.get(position)) {
                    selectedItems.delete(position);
                } else {
                    selectedItems.delete(prePosition);
                    selectedItems.put(position, true);
                }

                if (prePosition != -1) {
                    notifyItemChanged(prePosition);
                    notifyItemChanged(position);
                    prePosition = position;
                }
            }
        }

        private void changeVisibility(final boolean isExpanded) {
            int dpValue = 150;
            float d = mContext.getResources().getDisplayMetrics().density;
            int height = (int) (dpValue * d);

            // ValueAnimator.ofInt(int... values)는 View가 변할 값을 지정, 인자는 int 배열
            ValueAnimator va = isExpanded ? ValueAnimator.ofInt(0, height) : ValueAnimator.ofInt(height, 0);

            // Animation이 실행되는 시간, n/1000초
            va.setDuration(600);
            va.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
                @Override
                public void onAnimationUpdate(@NonNull ValueAnimator animation) {
                    int value = (int) animation.getAnimatedValue();
                    useTime.getLayoutParams().height = value;
                    useTime.requestLayout();
                    useTime.setVisibility(isExpanded ? View.VISIBLE : View.GONE);
                }
            });
            va.start();
        }
    }
}
