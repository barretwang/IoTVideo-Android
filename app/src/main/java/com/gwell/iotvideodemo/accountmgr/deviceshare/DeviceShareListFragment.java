package com.gwell.iotvideodemo.accountmgr.deviceshare;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.google.android.material.snackbar.Snackbar;
import com.gwell.http.utils.HttpUtils;
import com.gwell.iotvideo.utils.LogUtils;
import com.gwell.iotvideodemo.R;
import com.gwell.iotvideodemo.base.BaseFragment;
import com.gwell.iotvideodemo.base.HttpRequestState;
import com.gwell.iotvideodemo.widget.ItemTouchHelperCallback;
import com.gwell.iotvideodemo.widget.OnMoveAndSwipedListener;

import java.util.ArrayList;
import java.util.List;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.Observer;
import androidx.lifecycle.ViewModelProviders;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

public class DeviceShareListFragment extends BaseFragment implements View.OnClickListener {
    private static final String TAG = "DeviceShareListFragment";

    private RecyclerView mRVShareList;
    private MyAdapter mAdapter;
    private List<ShareList.DataBean.User> mShareList;
    private DeviceShareViewModel mDeviceShareViewModel;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_device_share_list, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        mShareList = new ArrayList<>();
        mRVShareList = view.findViewById(R.id.share_list);
        view.findViewById(R.id.btn_fresh_list).setOnClickListener(this);
        mAdapter = new MyAdapter();
        mRVShareList.setLayoutManager(new LinearLayoutManager(getContext(), RecyclerView.VERTICAL, false));
        mRVShareList.setAdapter(mAdapter);
        ItemTouchHelper.Callback callback = new ItemTouchHelperCallback(mAdapter);
        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(callback);
        itemTouchHelper.attachToRecyclerView(mRVShareList);
        initViewModel();
    }

    private void initViewModel() {
        mDeviceShareViewModel = ViewModelProviders.of(getActivity()).get(DeviceShareViewModel.class);
        mDeviceShareViewModel.getQueryShareListData().observe(getActivity(), new Observer<HttpRequestState>() {
            @Override
            public void onChanged(HttpRequestState httpRequestState) {
                if (httpRequestState.getStatus() == HttpRequestState.Status.SUCCESS) {
                    ShareList shareList = HttpUtils.JsonToEntity(httpRequestState.getJsonObject().toString(), ShareList.class);
                    if (shareList != null && shareList.getData() != null) {
                        mShareList = shareList.getData().getUsers();
                        mAdapter.notifyDataSetChanged();
                    } else {
                        Snackbar.make(mRVShareList, httpRequestState.getStatusTip(), Snackbar.LENGTH_LONG).show();
                    }
                } else {
                    Snackbar.make(mRVShareList, httpRequestState.getStatusTip(), Snackbar.LENGTH_LONG).show();
                }
            }
        });
        mDeviceShareViewModel.getCancelShareData().observe(getActivity(), new Observer<DeviceShareViewModel.CancelShare>() {
            @Override
            public void onChanged(DeviceShareViewModel.CancelShare httpRequestState) {
                HttpRequestState.Status status = httpRequestState.httpRequestState.getStatus();
                LogUtils.i(TAG, "cancel share " + status);
                if (status == HttpRequestState.Status.SUCCESS) {
                    Snackbar.make(mRVShareList, "cancel share " + getString(R.string.success), Snackbar.LENGTH_LONG).show();
                    LogUtils.i(TAG, "cancel share position = " + httpRequestState.position);
                    mShareList.remove(httpRequestState.position);
                    mAdapter.notifyItemRemoved(httpRequestState.position);
                } else if (status == HttpRequestState.Status.ERROR) {
                    Snackbar.make(mRVShareList, "cancel share " + httpRequestState.httpRequestState.getStatusTip(), Snackbar.LENGTH_LONG).show();
                    mAdapter.notifyDataSetChanged();
                }
            }
        });
        mDeviceShareViewModel.listSharedUsers();
    }

    @Override
    public void onClick(View view) {
        if (view.getId() == R.id.btn_fresh_list) {
            mDeviceShareViewModel.listSharedUsers();
        }
    }

    private static class ItemHolder extends RecyclerView.ViewHolder {
        TextView accountName;

        ItemHolder(@NonNull View itemView) {
            super(itemView);
            accountName = itemView.findViewById(R.id.share_item_name);
        }
    }

    private class MyAdapter extends RecyclerView.Adapter<ItemHolder> implements OnMoveAndSwipedListener {
        @NonNull
        @Override
        public ItemHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(getContext()).inflate(R.layout.item_share, parent, false);
            ItemHolder holder = new ItemHolder(view);
            return holder;
        }

        @Override
        public void onBindViewHolder(@NonNull ItemHolder holder, int position) {
            String name = mShareList.get(position).getDisplayName();
            holder.accountName.setText(name);
        }

        @Override
        public int getItemCount() {
            return mShareList.size();
        }

        @Override
        public boolean onItemMove(int fromPosition, int toPosition) {
            return false;
        }

        @Override
        public void onItemDismiss(int position) {
            LogUtils.i(TAG, "onItemDismiss " + position);
            mDeviceShareViewModel.cancelShare(mShareList.get(position), position);
        }
    }
}
