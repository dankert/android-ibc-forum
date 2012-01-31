package de.mtbnews.android.tapatalk.wrapper;

import java.util.Date;
import java.util.List;

public class Mailbox implements ListEntry
{

	private String id;
	private String name;
	private int countAll;
	private int countUnread;
	public List<Message> messages;

	public Mailbox(String id, String name, Integer countAll, Integer countUnread)
	{
		this.id = id;
		this.name = name;
		this.countAll = countAll;
		this.countUnread = countUnread;
	}

	public String getId()
	{
		return id;
	}

	@Override
	public String getContent()
	{
		return null;
	}

	@Override
	public Date getDate()
	{
		return null;
	}

	@Override
	public String getName()
	{
		return name;
	}

	@Override
	public String getTitle()
	{
		return "" + countAll + " (" + countUnread + ")";
	}

}