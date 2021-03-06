/**
 * Copyright (C) 2015 JianyingLi <lijy91@foxmail.com>
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.daza.app.ui;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import org.greenrobot.eventbus.EventBus;
import org.greenrobot.eventbus.Subscribe;
import org.greenrobot.eventbus.ThreadMode;

import java.util.ArrayList;
import java.util.List;

import io.daza.app.R;
import io.daza.app.event.LoginStatusChangedEvent;
import io.daza.app.handler.ErrorHandler;
import io.daza.app.model.Notification;
import io.daza.app.model.Pagination;
import io.daza.app.model.Result;
import io.daza.app.ui.base.BaseListFragment;
import io.daza.app.ui.vh.NotificationViewHolder;
import io.daza.app.util.Auth;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

import static io.daza.app.api.ApiClient.API;

public class HomeInboxFragment extends
        BaseListFragment<NotificationViewHolder, Notification, Result<List<Notification>>> {
    private final String TAG = HomeInboxFragment.class.getSimpleName();

    public HomeInboxFragment() {
        // Required empty public constructor
    }

    public static HomeInboxFragment newInstance() {
        HomeInboxFragment fragment = new HomeInboxFragment();
        Bundle args = new Bundle();
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        EventBus.getDefault().register(this);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_home_inbox, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        super.initLoader();
    }

    @Override
    public void onDestroy() {
        EventBus.getDefault().unregister(this);
        super.onDestroy();
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        inflater.inflate(R.menu.menu_home_inbox, menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == R.id.action_mark_as_read) {
            if (!Auth.check()) {
                return false;
            }
            API.markAsRead().enqueue(new Callback<Result>() {
                @Override
                public void onResponse(Call<Result> call, Response<Result> response) {
                    if (new ErrorHandler(getActivity()).handleErrorIfNeed(response.errorBody())) {
                        return;
                    }
                    if (response.isSuccessful()) {
                        for (int i = 0; i < getItemsSource().size(); i++) {
                            getItemsSource().get(i).setUnread(false);
                        }
                        getAdapter().notifyDataSetChanged();
                    }
                }

                @Override
                public void onFailure(Call<Result> call, Throwable t) {
                    new ErrorHandler(getActivity()).handleError(t);
                }
            });
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public NotificationViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View itemView = LayoutInflater.from(parent.getContext()).inflate(R.layout.fragment_home_inbox_list_item, parent, false);
        return new NotificationViewHolder(itemView);
    }

    @Override
    public void onBindViewHolder(NotificationViewHolder holder, int position) {
        super.onBindViewHolder(holder, position);
        Notification data = getItemsSource().get(position);
        holder.bind(data);
    }

    @Override
    public Result<List<Notification>> onLoadInBackground() throws Exception {
        if (!Auth.check()) {
            Result<List<Notification>> result = new Result<>();
            result.setCode(0);
            result.setPagination(new Pagination());
            result.setData(new ArrayList<Notification>());
            return result;
        }
        return API.getNotifications(getNextPage(), null).execute().body();
    }

    @Override
    public void onLoadComplete(Result<List<Notification>> data) {
        if (data != null && data.isSuccessful()) {
            setPagination(data.getPagination());
            if (data.getPagination().getCurrent_page() == 1) {
                getItemsSource().clear();
            }
            getItemsSource().addAll(data.getData());
        }
        super.onRefreshComplete();
    }

    @Override
    protected void onListItemClick(RecyclerView rv, View v, int position, long id) {
        Notification data = getItemsSource().get(position);

        Intent intent = null;

        switch (data.getReason()) {
            case "followed":
                intent = new Intent(getActivity(), UserDetailActivity.class);
                intent.putExtra("extra_user_id", data.getFrom_user_id());
                intent.putExtra("extra_user", data.getFrom_user().toJSONString());
                break;
            case "subscribed":
                intent = new Intent(getActivity(), TopicDetailActivity.class);
                intent.putExtra("extra_topic_id", data.getTopic_id());
                intent.putExtra("extra_topic", data.getTopic().toJSONString());
                break;
            case "upvoted":
                intent = new Intent(getActivity(), ArticleDetailActivity.class);
                intent.putExtra("extra_article_id", data.getArticle_id());
                intent.putExtra("extra_article", data.getArticle().toJSONString());
                break;
            case "comment":
                intent = new Intent(getActivity(), ArticleDetailActivity.class);
                intent.putExtra("extra_article_id", data.getArticle_id());
                intent.putExtra("extra_article", data.getArticle().toJSONString());
                break;
            case "mention":
                intent = new Intent(getActivity(), ArticleDetailActivity.class);
                intent.putExtra("extra_article_id", data.getArticle_id());
                intent.putExtra("extra_article", data.getArticle().toJSONString());
                break;
            default:
                return;
        }
        startActivity(intent);
    }

    @Subscribe(threadMode = ThreadMode.MAIN)
    public void onEvent(LoginStatusChangedEvent event) {
        Log.d(TAG, "onLoginStatusChangedEvent");
        if (!Auth.check()) {
            getItemsSource().clear();
            super.onRefreshComplete();
        } else {
            firstRefresh();
        }
    }
}
