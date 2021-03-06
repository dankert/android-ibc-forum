/**
 * 
 */
package de.mtbnews.android;

import java.io.IOException;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.util.Log;
import android.view.ContextMenu;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ContextMenu.ContextMenuInfo;
import android.view.View.OnCreateContextMenuListener;
import android.widget.AdapterView;
import android.widget.ListAdapter;
import android.widget.ListView;
import android.widget.Toast;
import android.widget.AdapterView.OnItemClickListener;
import de.mtbnews.android.adapter.ListEntryContentAdapter;
import de.mtbnews.android.tapatalk.TapatalkClient;
import de.mtbnews.android.tapatalk.TapatalkException;
import de.mtbnews.android.tapatalk.wrapper.Forum;
import de.mtbnews.android.tapatalk.wrapper.Topic;
import de.mtbnews.android.util.IBC;
import de.mtbnews.android.util.IBCException;
import de.mtbnews.android.util.ServerAsyncTask;
import de.mtbnews.android.util.Utils;

/**
 * @author dankert
 * 
 */
public class ForumActivity extends EndlessListActivity<Topic>
{
	public static final String FORUM_ID = "forum_id";

	private SharedPreferences prefs;
	private int totalSize;
	private String forumId;
	private int topicMode = TapatalkClient.TOPIC_STANDARD;

	private TapatalkClient client;

	/**
	 * Diese Liste immer von oben beginnen.
	 * 
	 * @see de.mtbnews.android.EndlessListActivity#isAutoScrolldown()
	 */
	@Override
	protected boolean isAutoScrolldown()
	{
		return false;
	}

	@Override
	protected void onCreate(Bundle savedInstanceState)
	{
		super.onCreate(savedInstanceState);

		setTheme(((IBCApplication) getApplication()).themeResId);
		setContentView(R.layout.listing);

		prefs = PreferenceManager.getDefaultSharedPreferences(this);

		client = ((IBCApplication) getApplication()).getTapatalkClient();

		if (!client.loggedIn && prefs.getBoolean("auto_login", false))
		{
			login();
		}

		forumId = getIntent().getStringExtra(FORUM_ID);
		Log.d(IBC.TAG, "Loading forum #" + forumId);

		ListAdapter adapter = new ListEntryContentAdapter(ForumActivity.this, entries);
		setListAdapter(adapter);
		initialLoad();

		// TODO: ggf. das hier in die Oberklasse?
		ListView list = getListView();
		list.setOnCreateContextMenuListener(new OnCreateContextMenuListener()
		{

			@Override
			public void onCreateContextMenu(ContextMenu menu, View v, ContextMenuInfo menuInfo)
			{
				if (topicMode == TapatalkClient.TOPIC_ANNOUNCEMENT)
					return;

				MenuInflater menuInflater = new MenuInflater(getApplication());
				menuInflater.inflate(R.menu.topic_context, menu);

			}
		});
		list.setOnItemClickListener(new OnItemClickListener()
		{
			@Override
			public void onItemClick(AdapterView<?> parent, View view, int position, long id)
			{
				if (topicMode == TapatalkClient.TOPIC_ANNOUNCEMENT)
					return;

				// int aktPosition = displayFrom + position + 1;
				final Intent intent = new Intent(ForumActivity.this, TopicActivity.class);
				Topic topic = ForumActivity.super.entries.get(position);
				intent.putExtra(TopicActivity.TOPIC_ID, topic.getId());
				startActivity(intent);
			}
		});
	}

	// TODO: Das auch in die anderen Listviews einbauen.
	@Override
	public boolean onContextItemSelected(MenuItem item)
	{
		AdapterView.AdapterContextMenuInfo menuInfo = (AdapterView.AdapterContextMenuInfo) item.getMenuInfo();

		switch (item.getItemId())
		{
			case R.id.menu_goto_top:

				final Intent intent = new Intent(ForumActivity.this, TopicActivity.class);
				intent.putExtra("topic_id", super.entries.get(menuInfo.position).getId());
				intent.putExtra("first_post", true);
				startActivity(intent);
				return true;

			case R.id.menu_goto_bottom:

				final Intent intent2 = new Intent(ForumActivity.this, TopicActivity.class);
				intent2.putExtra("topic_id", super.entries.get(menuInfo.position).getId());
				intent2.putExtra("last_post", true);
				startActivity(intent2);
				return true;
		}

		return super.onContextItemSelected(item);
	}

	private void login()
	{
		new ServerAsyncTask(this, R.string.waitingfor_login)
		{

			@Override
			protected synchronized void callServer() throws IOException, IBCException
			{

				try
				{
					client.login(prefs.getString("username", ""), prefs.getString("password", ""));

				}
				catch (TapatalkException e)
				{
					throw new IBCException(R.string.login_failed, e.getMessage(), e);
				}

			}

		}.executeSynchronized();
	}

	private void logout()
	{
		new ServerAsyncTask(this, R.string.waitingfor_logout)
		{

			@Override
			protected synchronized void callServer() throws IOException, IBCException
			{

				try
				{
					client.logout();

				}
				catch (TapatalkException e)
				{

				}

			}

		}.executeSynchronized();
	}

	@Override
	public boolean onCreateOptionsMenu(Menu menu)
	{
		super.onCreateOptionsMenu(menu);
		MenuInflater mi = new MenuInflater(getApplication());

		if (client.loggedIn)
			mi.inflate(R.menu.forum, menu);
		else
			mi.inflate(R.menu.forum_guest, menu);

		return true;
	}

	public boolean onOptionsItemSelected(MenuItem item)
	{
		switch (item.getItemId())
		{
			case R.id.menu_preferences:
				startActivity(new Intent(this, Configuration.class));
				return true;

			case R.id.menu_mailbox:
				startActivity(new Intent(this, MailboxActivity.class));
				return true;

			case R.id.menu_create_topic:
				Intent intent5 = new Intent(this, CreateTopicActivity.class);
				intent5.putExtra(FORUM_ID, forumId);
				startActivity(intent5);
				return true;

			case R.id.menu_subscribed_forums:
				startActivity(new Intent(this, SubscriptionForenActivity.class));
				return true;

			case R.id.menu_subscribed_topics:
				startActivity(new Intent(this, SubscriptionTopicsActivity.class));
				return true;

			case R.id.menu_participated_topics:
				Intent intent = new Intent(this, SearchActivity.class);
				intent.setAction(SearchActivity.ACTION_SEARCH_PARTICIPATED_TOPICS);
				startActivity(intent);
				return true;

			case R.id.menu_latest_topics:
				Intent intent2 = new Intent(this, SearchActivity.class);
				intent2.setAction(SearchActivity.ACTION_SEARCH_LATEST_TOPICS);
				startActivity(intent2);
				return true;
			case R.id.menu_unread_topics:
				Intent intent3 = new Intent(this, SearchActivity.class);
				intent3.setAction(SearchActivity.ACTION_SEARCH_UNREAD_TOPICS);
				startActivity(intent3);
				return true;

			case R.id.menu_logout:
				logout();
				return true;

			case R.id.menu_login:

				if (TextUtils.isEmpty(prefs.getString("username", "")))
				{
					Toast.makeText(this, R.string.nousername, Toast.LENGTH_LONG).show();

					Intent intent4 = new Intent(this, Configuration.class);
					startActivity(intent4);
				}
				// Evtl. gibt es jetzt einen Benutzernamen ...

				if (!TextUtils.isEmpty(prefs.getString("username", "")))
				{
					login();
				}
				else
				{
					Toast.makeText(this, R.string.nousername, Toast.LENGTH_LONG).show();
				}

				return true;

			case R.id.menu_subscribe:

				AlertDialog.Builder builder = new AlertDialog.Builder(this);
				builder.setTitle(R.string.subscribe_forum);
				builder.setItems(R.array.subscription_modes, new DialogInterface.OnClickListener()
				{
					public void onClick(DialogInterface dialog, final int item)
					{

						new ServerAsyncTask(ForumActivity.this, R.string.subscribe_forum)
						{

							@Override
							protected void callServer() throws IOException, TapatalkException
							{
								client.subscribeForum(forumId, item - 1);
							}

							@Override
							protected void doOnSuccess()
							{
								Toast
										.makeText(getApplicationContext(), R.string.subscription_saved,
												Toast.LENGTH_SHORT).show();
							}
						}.execute();
					}
				});
				builder.create().show();
				return true;
			case R.id.menu_mark_read:

				new ServerAsyncTask(ForumActivity.this, R.string.mark_forum_read)
				{

					@Override
					protected void callServer() throws TapatalkException
					{
						client.markForumAsRead(forumId);
					}

					@Override
					protected void doOnSuccess()
					{
					}
				}.execute();

				return true;

			case R.id.menu_mark_all_read:

				new ServerAsyncTask(ForumActivity.this, R.string.mark_forum_read)
				{
					@Override
					protected void callServer() throws TapatalkException
					{
						client.markForumAsRead(null);
					}

					@Override
					protected void doOnSuccess()
					{
					}
				}.execute();

				return true;

			case R.id.menu_mode:

				// Den Topic-Mode ändern (Standard,Wichtig,Ankündigungen)
				AlertDialog.Builder builder2 = new AlertDialog.Builder(this);
				builder2.setTitle(R.string.mode);
				builder2.setItems(R.array.topic_modes, new DialogInterface.OnClickListener()
				{

					public void onClick(DialogInterface dialog, final int item)
					{
						topicMode = item + 1;
						initialLoad();
					}
				});
				builder2.create().show();
				return true;
		}
		return false;
	}

	@Override
	protected int getTotalSize()
	{
		return totalSize;
	}

	@Override
	protected void loadEntries(final de.mtbnews.android.EndlessListActivity.OnListLoadedListener<Topic> onListLoaded,
			final int from, final int to, final boolean firstLoad)
	{

		new ServerAsyncTask(this, R.string.waitingfor_forum)
		{
			private Forum forum;

			@Override
			protected void callServer() throws TapatalkException
			{
				if (Utils.loginExceeded(client))
					client.login(prefs.getString("username", ""), prefs.getString("password", ""));

				this.forum = client.getForum(forumId, from, to, topicMode);
				totalSize = this.forum.topicCount;
			}

			protected void doOnSuccess()
			{
				ForumActivity.this.setTitle(forum.getTitle());
				onListLoaded.listLoaded(this.forum.getTopics());

				if (firstLoad)
					if (prefs.getBoolean("show_hints", true))
						Toast.makeText(ForumActivity.this, R.string.hint_press_long, Toast.LENGTH_SHORT).show();
			}

		}.execute();

	}

}
