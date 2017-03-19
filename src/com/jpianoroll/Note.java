package com.jpianoroll;

import javafx.scene.shape.Rectangle;

/*
 * @author pochey 
 */

public class Note extends Rectangle
{
	private boolean on;
	
	public Note(double x, double y, double width, double height)
	{
		super(x, y, width, height);
		on = false;
	}

	public boolean isOn()
	{
		return on;
	}

	public void setOn()
	{
		on = true;
	}
	
	public void setOff()
	{
		on = false;
	}
}
