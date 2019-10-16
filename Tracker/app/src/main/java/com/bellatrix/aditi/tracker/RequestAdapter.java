package com.bellatrix.aditi.tracker;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;

import androidx.recyclerview.widget.RecyclerView;

import com.bellatrix.aditi.tracker.DatabaseClasses.Request;

import java.util.List;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.FolderViewHolder> {

    private int type;
    private List<Request> requests;

    RequestAdapter(int type, List<Request> requests)
    {
        this.type = type;
        this.requests = requests;
    }

    @Override
    public FolderViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.request_holder, parent, false);

        FolderViewHolder caseViewHolder = new FolderViewHolder(view);
        return caseViewHolder;
    }

    @Override
    public void onBindViewHolder(FolderViewHolder holder, int position) {
        holder.bind(position);
    }

    @Override
    public int getItemCount() {
        return requests.size();
    }

    public class FolderViewHolder extends RecyclerView.ViewHolder {

        TextView user, name;
        Button cancel, accept;
        FolderViewHolder(View view)
        {
            super(view);
            user = (TextView) view.findViewById(R.id.tv_user);
            name = (TextView) view.findViewById(R.id.tv_name);
            cancel = (Button) view.findViewById(R.id.button_cancel);
            accept = (Button) view.findViewById(R.id.button_accept);
        }
        void bind(int position)
        {
            this.user.setText(requests.get(position).getUser());
            this.name.setText(requests.get(position).getName());

            //sent requests
            if(type == 1) {
                cancel.setVisibility(View.GONE);
                accept.setVisibility(View.GONE);
            }
        }
    }
//    public void swapCursor(Cursor cursor)
//    {
//        if(mCursor!=null)
//            mCursor.close();
//
//        mCursor = cursor;
//        if(mCursor!=null)
//        {
//            this.notifyDataSetChanged();
//        }
//    }
}
