package com.jpianoroll;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.util.ArrayList;
import java.util.Scanner;

import javax.sound.midi.InvalidMidiDataException;
import javax.sound.midi.MidiEvent;
import javax.sound.midi.MidiMessage;
import javax.sound.midi.MidiSystem;
import javax.sound.midi.MidiUnavailableException;
import javax.sound.midi.Sequence;
import javax.sound.midi.Sequencer;
import javax.sound.midi.Track;

import javafx.animation.AnimationTimer;
import javafx.application.Application;
import javafx.application.Platform;
import javafx.beans.value.ChangeListener;
import javafx.beans.value.ObservableValue;
import javafx.event.ActionEvent;
import javafx.event.EventHandler;
import javafx.geometry.Insets;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.ColorPicker;
import javafx.scene.control.Label;
import javafx.scene.control.Slider;
import javafx.scene.control.Tooltip;
import javafx.scene.input.KeyCode;
import javafx.scene.input.KeyEvent;
import javafx.scene.layout.ColumnConstraints;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.scene.paint.Color;
import javafx.scene.shape.Rectangle;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.WindowEvent;

/*
 * @author pochey 
 */

public class JPianoRollFX extends Application
{
	public static final String CONFIG_FILE = "config.txt";
	
//	public static final int SCREEN_WIDTH = 1280;
//	public static final int SCREEN_HEIGHT = 1024;
	
	public static int SCREEN_WIDTH = 800;
	public static int SCREEN_HEIGHT = 500;
	
	//top bound = 127.0
	public static float UPPER_BOUNDS;
	public static float LOWER_BOUNDS;
	public static int BOUNDS_PADDING = 10;
	private float originalLowerBounds;
	private float originalUpperBounds;
	
	public static final int NON_RECT_NODES = 0;
	
	public static double SPEED = 3;
	private int rectHeight;
	private int rectWidth = 1;
	private boolean running;
	private boolean midiSet;
	private ArrayList<OnNote> onNotes;
	private Color[] colors;

	private Group root;
	private Scene scene;
	private Sequence sequence = null;
	private Sequencer sequencer = null;
	private File file;
	private File originalFile;
	private AnimationTimer mainTimer;
	private GridPane gridPane;
	private JPianoReceiver receiver;
		
	public static void main(String[] args)
	{
		launch(args);
	}
	
	private void midiInit() throws MidiUnavailableException, InvalidMidiDataException, IOException
	{
		onNotes = new ArrayList<OnNote>();
		running = false;
		
		sequence = MidiSystem.getSequence(file);
		
		establishBounds();
		
		sequencer = MidiSystem.getSequencer();
		sequencer.open();
		sequencer.setSequence(sequence);
		
		if (receiver == null)
			receiver = new JPianoReceiver();
		
		sequencer.getTransmitter().setReceiver(receiver);
		
		rectHeight = (int)(1.0 / (UPPER_BOUNDS - LOWER_BOUNDS) * SCREEN_HEIGHT); 
		
		if (!midiSet)
			initUIPostMidi();
		
		midiSet = true;
	}
	
	private void initUIPostMidi()
	{
		Slider positionSlider = new Slider(0, sequencer.getTickLength(), sequencer.getTickPosition());
		positionSlider.setTooltip(new Tooltip("Position Slider"));
		
		Label positionValue = new Label(Long.toString(sequencer.getTickPosition()));
		
		positionSlider.valueProperty().addListener(new ChangeListener<Number>()
		{

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				sequencer.setTickPosition(newValue.longValue());
				positionValue.setText(Long.toString(newValue.longValue()));
				root.getChildren().remove(NON_RECT_NODES, root.getChildren().size());
				stopPlayBack();
			}
			
		});
		
		Slider tempoSlider = new Slider(0, 400, sequencer.getTempoInBPM());
		tempoSlider.setTooltip(new Tooltip("Tempo Slider"));
		
		Label tempoValue = new Label(Integer.toString((int)sequencer.getTempoInBPM()));
		
		tempoSlider.valueProperty().addListener(new ChangeListener<Number>()
		{

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				sequencer.setTempoInBPM(newValue.intValue());
				tempoValue.setText(Integer.toString(newValue.intValue()));
			}
			
		});
		
		gridPane.add(positionSlider, 0, 4);
		gridPane.add(positionValue, 1, 4);
		gridPane.add(tempoSlider, 0, 5);
		gridPane.add(tempoValue, 1, 5);
	}
	
	private void establishBounds()
	{
		UPPER_BOUNDS = 0;
		LOWER_BOUNDS = SCREEN_HEIGHT;
		Track[] tracks = sequence.getTracks();
		MidiEvent e = null;
		MidiMessage m = null;
		
		for (int i = 0; i < tracks.length; i++)
		{
			for (int j = 0; j < tracks[i].size(); j++)
			{
				e = tracks[i].get(j);
				m = e.getMessage();
				
				if ((m.getMessage()[0] & 0xF0) == 0x90)
				{
					byte n = m.getMessage()[1];
					if (n > UPPER_BOUNDS)
						UPPER_BOUNDS = n;
					if (n < LOWER_BOUNDS)
						LOWER_BOUNDS = n;
				}
			}
		}
		originalUpperBounds = UPPER_BOUNDS;
		originalLowerBounds = LOWER_BOUNDS;
		UPPER_BOUNDS += BOUNDS_PADDING;
		LOWER_BOUNDS -= BOUNDS_PADDING;
		System.out.println("Upper Bounds: " + UPPER_BOUNDS);
		System.out.println("Lower Bounds: " + LOWER_BOUNDS);
	}

	@Override
	public void start(Stage primaryStage)
	{		
		midiSet = false;
		gridPane = new GridPane();
		colors = new Color[16];
		
		try
		{
			readConfig();
		} 
		catch (Exception e)
		{
			System.out.println("Could not read config file.");
			if (receiver != null)
				receiver.setColor(Color.ALICEBLUE, 0);
		}
		
		try
		{
			midiInit();
		} 
		catch (Exception e)
		{
			System.out.println("Midi sequencer not initialized - select a midi file from options");
		}
		
		if (receiver != null)
			receiver.setColors(colors);
		
		initUI(primaryStage);    
        
        mainTimer =  new AnimationTimer()
        {

			@Override
			public void handle(long now)
			{	
				//thread safe note adding
				if (receiver.getAddNote() > 0)
				{
					for (int i = 0; i < receiver.getAddNote(); i++)
					{
						byte track = receiver.getOnMessages().get(i).getMessage()[0];
						byte note = receiver.getOnMessages().get(i).getMessage()[1];
						float keyFraction = (note - LOWER_BOUNDS) / (UPPER_BOUNDS - LOWER_BOUNDS);
						double rawHeight = SCREEN_HEIGHT * keyFraction;
						double y = SCREEN_HEIGHT - rawHeight;
						Note r = null;
						boolean shouldAdd = false;
						boolean worked = false;
						
						for (int j = 0; j < root.getChildren().size() - NON_RECT_NODES; j++)
						{
							r = (Note)root.getChildren().get(NON_RECT_NODES + j);
							if (!r.isOn())
							{
								r.setTranslateX(0);
								r.setX(SCREEN_WIDTH);
								r.setY(y);
								r.setWidth(rectWidth);
								r.setHeight(rectHeight);
								worked = true;
								break;
							}
						}

						if (!worked)
						{
							r = new Note(SCREEN_WIDTH, y, rectWidth, rectHeight);
							shouldAdd = true;
						}
						
						r.setOn();
						
						r.setFill(receiver.getColors()[track & 0x0F]);
						
						if (shouldAdd)
							root.getChildren().add(r);
						
						onNotes.add(new OnNote(r, note));
					}
					receiver.clearAddNote();
					receiver.getOnMessages().clear();
				}
				
				//animation
				if (root.getChildren().size() > NON_RECT_NODES)
					for (Node node : root.getChildren())
						if (node instanceof Rectangle)
							node.setTranslateX(node.getTranslateX() - SPEED);
				
				for (int i = 0; i < onNotes.size(); i++)	
					onNotes.get(i).getRectangle().setWidth(-onNotes.get(i).getRectangle().getTranslateX());
				
				//offNotes	
				for (int i = 0; i < receiver.getOffMessages().size(); i++)
				{
					byte b = receiver.getOffMessages().get(i).getMessage()[1];
					for (int j = 0; j < onNotes.size(); j++)
					{
						if (onNotes.get(j).getKey() == b)
						{
							onNotes.remove(j);
							break;
						}
					}
				}
				 
 				receiver.getOffMessages().clear();
				
				collision();
			}
        	
        };
	}
	
	private void initUI(Stage primaryStage)
	{
		root = new Group();
    	scene = new Scene(root, SCREEN_WIDTH, SCREEN_HEIGHT, Color.BLACK);
        primaryStage.setTitle("JPianoRoll");
        primaryStage.setScene(scene);
        
        primaryStage.show();
		
		scene.getWindow().onCloseRequestProperty().set(new EventHandler<WindowEvent>()		
		{

			@Override
			public void handle(WindowEvent event)
			{
				if (sequencer != null)
					sequencer.close();
				writeConfig();
				primaryStage.close();
				Platform.exit();
				System.out.println("Program closed");
				System.exit(0);
			}
	
		});
		ColumnConstraints column1 = new ColumnConstraints();
		column1.setPercentWidth(90);
		ColumnConstraints column2 = new ColumnConstraints();
		column2.setPercentWidth(10);
		gridPane.getColumnConstraints().addAll(column1, column2);
		
		gridPane.setHgap(30);
		
		final Stage optionStage = new Stage();
		optionStage.initModality(Modality.APPLICATION_MODAL);
		optionStage.initOwner(primaryStage);
		VBox vbox = new VBox();
		
		final Slider speedSlider = new Slider(0, 20, SPEED);
		speedSlider.setShowTickLabels(true);
		speedSlider.setShowTickMarks(true);
		speedSlider.setTooltip(new Tooltip("Animation Speed"));
		
		final Label speedValue = new Label(Integer.toString((int)SPEED));
		
		speedSlider.valueProperty().addListener(new ChangeListener<Number>()
		{

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				SPEED = newValue.doubleValue();
				speedValue.setText(Integer.toString((int)SPEED));
			}
			
		});
		
		Slider paddingSlider = new Slider(0, 127, BOUNDS_PADDING);
		paddingSlider.setTooltip(new Tooltip("Padding Slider"));
		paddingSlider.setShowTickLabels(true);
		paddingSlider.setShowTickMarks(true);
		
		Label paddingValue = new Label(Integer.toString(BOUNDS_PADDING));
		
		paddingSlider.valueProperty().addListener(new ChangeListener<Number>()
		{

			@Override
			public void changed(ObservableValue<? extends Number> observable, Number oldValue, Number newValue)
			{
				BOUNDS_PADDING = newValue.intValue();
				paddingValue.setText(Integer.toString(newValue.intValue()));
				UPPER_BOUNDS = originalUpperBounds;
				LOWER_BOUNDS = originalLowerBounds;
				UPPER_BOUNDS += BOUNDS_PADDING;
				LOWER_BOUNDS -= BOUNDS_PADDING;
				rectHeight = (int)(1.0 / (UPPER_BOUNDS - LOWER_BOUNDS) * SCREEN_HEIGHT);
			}
			
		});
		
		final Button colorButton = new Button("Open color options");
		
		colorButton.setOnAction(new EventHandler<ActionEvent>(){

			@Override
			public void handle(ActionEvent event)
			{
				if (receiver != null)
				{
					final Stage colorStage = new Stage();
					colorStage.initModality(Modality.APPLICATION_MODAL);
					colorStage.initOwner(primaryStage);
					VBox colorBox = new VBox();
					
					for (int i = 0; i < 16; i++)
					{
						final ColorPicker colorPicker = new ColorPicker(receiver.getColors()[i]);
						final int index = i;
						colorPicker.setOnAction(new EventHandler<ActionEvent>() {
							@Override
							public void handle(ActionEvent event)
							{
								receiver.setColor(colorPicker.getValue(), index);
							}
						});
						
						colorBox.getChildren().add(colorPicker);
					}
					
					Scene colorScene = new Scene(colorBox, 140, SCREEN_HEIGHT / 2 - 100);
					colorStage.setScene(colorScene);
					colorStage.setTitle("Color options");
					colorStage.show();
				}
				else
					System.out.println("Select a midi file first");
			}
			
		});
		
		
		final Button fileButton = new Button("Open midi file");
		
		fileButton.setOnAction(new EventHandler<ActionEvent>(){

			@Override
			public void handle(ActionEvent event)
			{
				FileChooser fileChooser = new FileChooser();
				if (file != null)
					fileChooser.setInitialDirectory(file.getParentFile());
				originalFile = file;
				file = fileChooser.showOpenDialog(primaryStage);
				if (file != null)
				{
					try
					{
						if (receiver != null)
							stopPlayBack(); 
						midiInit();
						root.getChildren().remove(NON_RECT_NODES, root.getChildren().size());
					}
					catch(Exception e)
					{
						file = originalFile;
						System.out.println("Could not open midi file");
						e.printStackTrace();
					}
				}
			}
			
		});
		
		gridPane.add(speedSlider, 0, 0);
		gridPane.add(speedValue, 1, 0);
		gridPane.add(paddingSlider, 0, 1);
		gridPane.add(paddingValue, 1, 1);
		gridPane.add(colorButton, 0, 2);
		gridPane.add(fileButton, 0, 3);
		
		vbox.getChildren().add(gridPane);
		vbox.setPadding(new Insets(10));
		Scene optionScene = new Scene(vbox, SCREEN_WIDTH / 2, 200);
		optionStage.setScene(optionScene);
		optionStage.setTitle("Options");
        
        scene.setOnKeyPressed(new EventHandler<KeyEvent>(){

			@Override
			public void handle(KeyEvent event)
			{
				if (midiSet && event.getCode() == KeyCode.SPACE)
				{
					if (!running)
					{
						SCREEN_WIDTH = (int) primaryStage.getWidth();
						SCREEN_HEIGHT = (int) primaryStage.getHeight();
						rectHeight = (int)(1.0 / (UPPER_BOUNDS - LOWER_BOUNDS) * SCREEN_HEIGHT);
						mainTimer.start();
						sequencer.start();
						running = true;
					}
					else
						stopPlayBack();
				}
				if (midiSet && !running && event.getCode() == KeyCode.BACK_SPACE)
				{
					stopPlayBack();
					root.getChildren().remove(NON_RECT_NODES, root.getChildren().size());
					sequencer.setTickPosition(0);
				}
				if (event.getCode() == KeyCode.TAB)
					optionStage.show();
				if (event.getCode() == KeyCode.C)
        		{
        			System.out.println("Notes on screen: " + (root.getChildren().size() - NON_RECT_NODES));
        			System.out.println("OnNotes: " + onNotes.size());
        		}
			}
        	
        });
	}
	
	private void stopPlayBack()
	{
		receiver.clearAddNote();
		receiver.getOnMessages().clear();
		receiver.getOffMessages().clear();
		onNotes.clear();
		mainTimer.stop();
		sequencer.stop();
		running = false;
	}
	
	private int margin = 200;
	
	private void collision()
	{
		Note t = null;
		if (root.getChildren().size() > NON_RECT_NODES)
		{
			for (int i = 0; i < root.getChildren().size() - NON_RECT_NODES; i++)
			{
				t = (Note)root.getChildren().get(NON_RECT_NODES + i);
				if (-t.getTranslateX() - t.getWidth() - margin > SCREEN_WIDTH)
					t.setOff();
			}
		}
	}
	
	private void writeConfig()
	{
		PrintWriter printWriter = null;
		try
		{
			printWriter = new PrintWriter(new File(CONFIG_FILE));
		} catch (FileNotFoundException e)
		{
			e.printStackTrace();
			System.exit(0);
		}
		printWriter.println(SPEED);
		printWriter.println(BOUNDS_PADDING);
		if (receiver != null)
		{ 
			Color[] colors = receiver.getColors();
			for (int i = 0; i < colors.length; i++)
				printWriter.println(colors[i].getRed() + " " + colors[i].getGreen() + " " + colors[i].getBlue() + " " + colors[i].getOpacity());
			if (file != null)
				printWriter.println(file.getAbsolutePath());
		}
		printWriter.close();
		System.out.println("Config file written");
	}
	
	private void readConfig() throws Exception
	{
		Scanner scanner = new Scanner(new File(CONFIG_FILE));
		
		SPEED = scanner.nextDouble();
		BOUNDS_PADDING = scanner.nextInt();
		
		for (int i = 0; i < colors.length; i++)
		{
			colors[i] = new Color(scanner.nextDouble(), scanner.nextDouble(), scanner.nextDouble(), scanner.nextDouble());
			scanner.nextLine();
		}
		String fileName = scanner.nextLine();
		try
		{
			file = new File(fileName);
		}
		catch(NullPointerException e)
		{
			file = null;
		}
		scanner.close();
		System.out.println("Config file read");
	}
}
