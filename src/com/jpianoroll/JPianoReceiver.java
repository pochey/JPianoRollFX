package com.jpianoroll;

import java.util.ArrayList;

import javax.sound.midi.MidiMessage;
import javax.sound.midi.Receiver;

import javafx.scene.paint.Color;

/*
 * @author pochey 
 */

public class JPianoReceiver implements Receiver
{
	private ArrayList<MidiMessage> onMessages;
	private ArrayList<MidiMessage> offMessages;
	private int addNote;
	private Color[] colors;
	
	public JPianoReceiver()
	{
		onMessages = new ArrayList<MidiMessage>();
		offMessages = new ArrayList<MidiMessage>();
		addNote = 0;
		colors = new Color[16];
		
		//default colors
		colors[0] = Color.ALICEBLUE;
		colors[1] = Color.AQUAMARINE; 
		colors[2] = Color.BLUEVIOLET;
		colors[3] = Color.BROWN;
		colors[4] = Color.CADETBLUE;
		colors[5] = Color.CRIMSON;
		colors[6] = Color.DARKCYAN;
		colors[7] = Color.FIREBRICK;
		colors[8] = Color.GOLD;
		colors[9] = Color.HOTPINK;
		colors[10] = Color.LIGHTSKYBLUE;
		colors[11] = Color.ORANGE;
		colors[12] = Color.TAN;
		colors[13] = Color.SPRINGGREEN;
		colors[14] = Color.SADDLEBROWN;
		colors[15] = Color.SEAGREEN;

	}

	@Override
	public void send(MidiMessage message, long timeStamp)
	{
		if((message.getMessage()[0] & 0xF0) == 0x80) //key off
			offMessages.add(message);
		if ((message.getMessage()[0] & 0xF0) == 0x90 && message.getMessage()[2] == 0)
			offMessages.add(message);
		else if ((message.getMessage()[0] & 0xF0) == 0x90) //key on
		{
			onMessages.add(message);
			incrementAddNote();
		}
	}
	
	/*
	 * Sets the color of the first track only
	 */
	public void setColor(Color color, int i)
	{
		colors[i] = color;
	}
	
	public void setColors(Color[] colors)
	{
		this.colors = colors;
	}
	
	public Color[] getColors()
	{
		return colors;
	}

	@Override
	public void close()
	{
		System.out.println("Receiver closed");
	}
	
	public int getAddNote()
	{
		return addNote;
	}
	
	public void clearAddNote()
	{
		addNote = 0;
	}
	
	public void incrementAddNote()
	{
		addNote++;
	}
	
	public ArrayList<MidiMessage> getOnMessages()
	{
		return onMessages;
	}

	public ArrayList<MidiMessage> getOffMessages()
	{
		return offMessages;
	}
}
