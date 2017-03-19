package com.jpianoroll;

import javafx.scene.shape.Rectangle;

/*
 * @author pochey 
 */

public class OnNote
{
	private Rectangle rectangle;
	private byte key;
	
	public OnNote(Rectangle rectangle, byte key)
	{
		this.rectangle = rectangle;
		this.key = key;
	}

	public Rectangle getRectangle()
	{
		return rectangle;
	}

	public byte getKey()
	{
		return key;
	}
}
