// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2020
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.controller.mcu;

import de.mossgrabers.controller.mcu.command.continuous.PlayPositionTempoCommand;
import de.mossgrabers.controller.mcu.command.pitchbend.PitchbendVolumeCommand;
import de.mossgrabers.controller.mcu.command.trigger.AssignableCommand;
import de.mossgrabers.controller.mcu.command.trigger.DevicesCommand;
import de.mossgrabers.controller.mcu.command.trigger.FaderTouchCommand;
import de.mossgrabers.controller.mcu.command.trigger.GrooveCommand;
import de.mossgrabers.controller.mcu.command.trigger.KeyCommand;
import de.mossgrabers.controller.mcu.command.trigger.KeyCommand.Key;
import de.mossgrabers.controller.mcu.command.trigger.MCUCursorCommand;
import de.mossgrabers.controller.mcu.command.trigger.MCURecordCommand;
import de.mossgrabers.controller.mcu.command.trigger.OverdubCommand;
import de.mossgrabers.controller.mcu.command.trigger.ScrubCommand;
import de.mossgrabers.controller.mcu.command.trigger.SelectCommand;
import de.mossgrabers.controller.mcu.command.trigger.SendSelectCommand;
import de.mossgrabers.controller.mcu.command.trigger.TempoTicksCommand;
import de.mossgrabers.controller.mcu.command.trigger.ToggleDisplayCommand;
import de.mossgrabers.controller.mcu.command.trigger.TracksCommand;
import de.mossgrabers.controller.mcu.command.trigger.ZoomCommand;
import de.mossgrabers.controller.mcu.controller.MCUAssignmentDisplay;
import de.mossgrabers.controller.mcu.controller.MCUColorManager;
import de.mossgrabers.controller.mcu.controller.MCUControlSurface;
import de.mossgrabers.controller.mcu.controller.MCUDisplay;
import de.mossgrabers.controller.mcu.controller.MCUSegmentDisplay;
import de.mossgrabers.controller.mcu.mode.BaseMode;
import de.mossgrabers.controller.mcu.mode.MarkerMode;
import de.mossgrabers.controller.mcu.mode.device.DeviceBrowserMode;
import de.mossgrabers.controller.mcu.mode.device.DeviceParamsMode;
import de.mossgrabers.controller.mcu.mode.track.MasterMode;
import de.mossgrabers.controller.mcu.mode.track.PanMode;
import de.mossgrabers.controller.mcu.mode.track.SendMode;
import de.mossgrabers.controller.mcu.mode.track.TrackMode;
import de.mossgrabers.controller.mcu.mode.track.VolumeMode;
import de.mossgrabers.framework.command.continuous.KnobRowModeCommand;
import de.mossgrabers.framework.command.core.NopCommand;
import de.mossgrabers.framework.command.trigger.AutomationCommand;
import de.mossgrabers.framework.command.trigger.BrowserCommand;
import de.mossgrabers.framework.command.trigger.MarkerCommand;
import de.mossgrabers.framework.command.trigger.ShiftCommand;
import de.mossgrabers.framework.command.trigger.application.DuplicateCommand;
import de.mossgrabers.framework.command.trigger.application.LayoutCommand;
import de.mossgrabers.framework.command.trigger.application.PaneCommand;
import de.mossgrabers.framework.command.trigger.application.PanelLayoutCommand;
import de.mossgrabers.framework.command.trigger.application.SaveCommand;
import de.mossgrabers.framework.command.trigger.application.UndoCommand;
import de.mossgrabers.framework.command.trigger.clip.NewCommand;
import de.mossgrabers.framework.command.trigger.device.DeviceOnOffCommand;
import de.mossgrabers.framework.command.trigger.mode.ButtonRowModeCommand;
import de.mossgrabers.framework.command.trigger.mode.ModeCursorCommand.Direction;
import de.mossgrabers.framework.command.trigger.mode.ModeSelectCommand;
import de.mossgrabers.framework.command.trigger.track.MoveTrackBankCommand;
import de.mossgrabers.framework.command.trigger.track.ToggleTrackBanksCommand;
import de.mossgrabers.framework.command.trigger.track.ToggleVUCommand;
import de.mossgrabers.framework.command.trigger.transport.MetronomeCommand;
import de.mossgrabers.framework.command.trigger.transport.PlayCommand;
import de.mossgrabers.framework.command.trigger.transport.PunchInCommand;
import de.mossgrabers.framework.command.trigger.transport.PunchOutCommand;
import de.mossgrabers.framework.command.trigger.transport.StopCommand;
import de.mossgrabers.framework.command.trigger.transport.TapTempoCommand;
import de.mossgrabers.framework.command.trigger.transport.ToggleLoopCommand;
import de.mossgrabers.framework.command.trigger.transport.WindCommand;
import de.mossgrabers.framework.configuration.AbstractConfiguration;
import de.mossgrabers.framework.configuration.ISettingsUI;
import de.mossgrabers.framework.controller.AbstractControllerSetup;
import de.mossgrabers.framework.controller.ButtonID;
import de.mossgrabers.framework.controller.ContinuousID;
import de.mossgrabers.framework.controller.ISetupFactory;
import de.mossgrabers.framework.controller.OutputID;
import de.mossgrabers.framework.controller.hardware.BindType;
import de.mossgrabers.framework.controller.hardware.IHwFader;
import de.mossgrabers.framework.controller.hardware.IHwRelativeKnob;
import de.mossgrabers.framework.controller.valuechanger.DefaultValueChanger;
import de.mossgrabers.framework.controller.valuechanger.RelativeEncoding;
import de.mossgrabers.framework.daw.IApplication;
import de.mossgrabers.framework.daw.ICursorDevice;
import de.mossgrabers.framework.daw.IHost;
import de.mossgrabers.framework.daw.IParameterBank;
import de.mossgrabers.framework.daw.ISendBank;
import de.mossgrabers.framework.daw.ITrackBank;
import de.mossgrabers.framework.daw.ITransport;
import de.mossgrabers.framework.daw.ModelSetup;
import de.mossgrabers.framework.daw.constants.AutomationMode;
import de.mossgrabers.framework.daw.data.IMasterTrack;
import de.mossgrabers.framework.daw.data.ITrack;
import de.mossgrabers.framework.daw.midi.IMidiAccess;
import de.mossgrabers.framework.daw.midi.IMidiInput;
import de.mossgrabers.framework.daw.midi.IMidiOutput;
import de.mossgrabers.framework.mode.Mode;
import de.mossgrabers.framework.mode.ModeManager;
import de.mossgrabers.framework.mode.Modes;
import de.mossgrabers.framework.view.ControlOnlyView;
import de.mossgrabers.framework.view.Views;

import java.util.Arrays;
import java.util.EnumMap;
import java.util.EnumSet;
import java.util.Map;
import java.util.Set;


/**
 * Support for the Mackie MCU protocol.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class MCUControllerSetup extends AbstractControllerSetup<MCUControlSurface, MCUConfiguration>
{
    /** State for button LED on. */
    public static final int                 MCU_BUTTON_STATE_ON  = 127;
    /** State for button LED off. */
    public static final int                 MCU_BUTTON_STATE_OFF = 0;

    private static final Map<Modes, String> MODE_ACRONYMS        = new EnumMap<> (Modes.class);

    static
    {
        MODE_ACRONYMS.put (Modes.TRACK, "TR");
        MODE_ACRONYMS.put (Modes.VOLUME, "VL");
        MODE_ACRONYMS.put (Modes.PAN, "PN");
        MODE_ACRONYMS.put (Modes.SEND1, "S1");
        MODE_ACRONYMS.put (Modes.SEND2, "S2");
        MODE_ACRONYMS.put (Modes.SEND3, "S3");
        MODE_ACRONYMS.put (Modes.SEND4, "S4");
        MODE_ACRONYMS.put (Modes.SEND5, "S5");
        MODE_ACRONYMS.put (Modes.SEND6, "S6");
        MODE_ACRONYMS.put (Modes.SEND7, "S7");
        MODE_ACRONYMS.put (Modes.SEND8, "S8");
        MODE_ACRONYMS.put (Modes.MASTER, "MT");
        MODE_ACRONYMS.put (Modes.DEVICE_PARAMS, "DC");
        MODE_ACRONYMS.put (Modes.BROWSER, "BR");
        MODE_ACRONYMS.put (Modes.MARKERS, "MK");
    }

    private static final Set<Modes> VALUE_MODES      = EnumSet.of (Modes.VOLUME, Modes.PAN, Modes.TRACK, Modes.SEND1, Modes.SEND2, Modes.SEND3, Modes.SEND4, Modes.SEND5, Modes.SEND6, Modes.SEND7, Modes.SEND8, Modes.DEVICE_PARAMS);

    private final int []            masterVuValues   = new int [2];
    private int                     masterFaderValue = -1;
    private final int []            vuValues         = new int [36];
    private final int []            faderValues      = new int [36];
    private final int               numMCUDevices;


    /**
     * Constructor.
     *
     * @param host The DAW host
     * @param factory The factory
     * @param globalSettings The global settings
     * @param documentSettings The document (project) specific settings
     * @param numMCUDevices The number of MCU devices (main device + extenders) to support
     */
    public MCUControllerSetup (final IHost host, final ISetupFactory factory, final ISettingsUI globalSettings, final ISettingsUI documentSettings, final int numMCUDevices)
    {
        super (factory, host, globalSettings, documentSettings);

        this.numMCUDevices = numMCUDevices;

        Arrays.fill (this.vuValues, -1);
        Arrays.fill (this.faderValues, -1);
        Arrays.fill (this.masterVuValues, -1);

        this.colorManager = new MCUColorManager ();
        this.valueChanger = new DefaultValueChanger (16241 + 1, 500, 20);
        this.configuration = new MCUConfiguration (host, this.valueChanger, factory.getArpeggiatorModes ());
    }


    /** {@inheritDoc} */
    @Override
    public void flush ()
    {
        super.flush ();

        this.surfaces.forEach (surface -> {
            final ModeManager modeManager = surface.getModeManager ();
            final Modes mode = modeManager.getActiveOrTempModeId ();
            this.updateMode (mode);

            if (mode == null)
                return;

            this.updateVUMeters ();
            this.updateFaders (surface.isShiftPressed ());
            this.updateSegmentDisplay ();

            final Mode activeOrTempMode = modeManager.getActiveOrTempMode ();
            if (activeOrTempMode instanceof BaseMode)
                ((BaseMode) activeOrTempMode).updateKnobLEDs ();
        });
    }


    /** {@inheritDoc} */
    @Override
    protected void createModel ()
    {
        final int adjustedNum = 8 * this.numMCUDevices;

        final ModelSetup ms = new ModelSetup ();
        ms.setNumTracks (adjustedNum);
        ms.setNumScenes (0);
        ms.setNumFilterColumnEntries (8);
        ms.setNumResults (8);
        ms.setNumParams (adjustedNum);
        ms.setNumDeviceLayers (0);
        ms.setNumDrumPadLayers (0);
        ms.setNumMarkers (adjustedNum);
        this.model = this.factory.createModel (this.colorManager, this.valueChanger, this.scales, ms);

        final ITrackBank trackBank = this.model.getTrackBank ();
        trackBank.setIndication (true);
        trackBank.addSelectionObserver ( (index, isSelected) -> this.handleTrackChange (isSelected));
    }


    /** {@inheritDoc} */
    @Override
    protected void createSurface ()
    {
        final IMidiAccess midiAccess = this.factory.createMidiAccess ();

        for (int i = 0; i < this.numMCUDevices; i++)
        {
            final IMidiOutput output = midiAccess.createOutput (i);
            final IMidiInput input = midiAccess.createInput (i, null);
            final MCUControlSurface surface = new MCUControlSurface (this.surfaces, this.host, this.colorManager, this.configuration, output, input, 8 * (this.numMCUDevices - i - 1), i == 0);
            this.surfaces.add (surface);
            surface.addTextDisplay (new MCUDisplay (this.host, output, true, false));
            surface.addTextDisplay (new MCUDisplay (this.host, output, false, i == 0));
            surface.addTextDisplay (new MCUSegmentDisplay (this.host, output));
            surface.addTextDisplay (new MCUAssignmentDisplay (this.host, output));
            surface.getModeManager ().setDefaultMode (Modes.VOLUME);
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void createModes ()
    {
        for (int index = 0; index < this.numMCUDevices; index++)
        {
            final MCUControlSurface surface = this.getSurface (index);
            final ModeManager modeManager = surface.getModeManager ();

            modeManager.registerMode (Modes.TRACK, new TrackMode (surface, this.model));
            modeManager.registerMode (Modes.VOLUME, new VolumeMode (surface, this.model));
            modeManager.registerMode (Modes.PAN, new PanMode (surface, this.model));
            final SendMode modeSend = new SendMode (surface, this.model);
            for (int i = 0; i < 8; i++)
                modeManager.registerMode (Modes.get (Modes.SEND1, i), modeSend);
            modeManager.registerMode (Modes.MASTER, new MasterMode (surface, this.model));

            modeManager.registerMode (Modes.DEVICE_PARAMS, new DeviceParamsMode (surface, this.model));
            modeManager.registerMode (Modes.BROWSER, new DeviceBrowserMode (surface, this.model));
            modeManager.registerMode (Modes.MARKERS, new MarkerMode (surface, this.model));
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void createObservers ()
    {
        for (int i = 0; i < this.numMCUDevices; i++)
        {
            final ModeManager mm = this.getSurface (i).getModeManager ();
            for (int j = 0; j < this.numMCUDevices; j++)
            {
                if (i != j)
                    this.getSurface (j).getModeManager ().addConnectedModeManagerListener (mm);
            }
        }

        this.model.getMasterTrack ().addSelectionObserver ( (index, isSelected) -> {
            if (isSelected)
                this.getSurface ().getModeManager ().setActiveMode (Modes.MASTER);
        });

        this.configuration.addSettingObserver (AbstractConfiguration.ENABLE_VU_METERS, () -> {
            for (int index = 0; index < this.numMCUDevices; index++)
            {
                final MCUControlSurface surface = this.getSurface (index);
                surface.switchVuMode (this.configuration.isEnableVUMeters () ? MCUControlSurface.VUMODE_LED_AND_LCD : MCUControlSurface.VUMODE_OFF);
                final Mode activeMode = surface.getModeManager ().getActiveOrTempMode ();
                if (activeMode != null)
                    activeMode.updateDisplay ();
                ((MCUDisplay) surface.getDisplay ()).forceFlush ();
            }
        });
    }


    /** {@inheritDoc} */
    @Override
    protected void createViews ()
    {
        for (int index = 0; index < this.numMCUDevices; index++)
        {
            final MCUControlSurface surface = this.getSurface (index);
            surface.getViewManager ().registerView (Views.CONTROL, new ControlOnlyView<> (surface, this.model));
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void registerTriggerCommands ()
    {
        // Assignments to the main device
        final MCUControlSurface surface = this.getSurface ();

        final ITransport t = this.model.getTransport ();
        final ICursorDevice cursorDevice = this.model.getCursorDevice ();

        // Footswitches
        this.addButton (ButtonID.FOOTSWITCH1, "Footswitch 1", new AssignableCommand (0, this.model, surface), MCUControlSurface.MCU_USER_A);
        this.addButton (ButtonID.FOOTSWITCH2, "Footswitch 2", new AssignableCommand (1, this.model, surface), MCUControlSurface.MCU_USER_B);

        // Navigation

        final WindCommand<MCUControlSurface, MCUConfiguration> rewindCommand = new WindCommand<> (this.model, surface, false);
        final WindCommand<MCUControlSurface, MCUConfiguration> forwardCommand = new WindCommand<> (this.model, surface, true);
        this.addButton (ButtonID.REWIND, "<<", rewindCommand, MCUControlSurface.MCU_REWIND, rewindCommand::isRewinding);
        this.addButton (ButtonID.FORWARD, ">>", forwardCommand, MCUControlSurface.MCU_FORWARD, forwardCommand::isForwarding);
        this.addButton (ButtonID.LOOP, "Loop", new ToggleLoopCommand<> (this.model, surface), MCUControlSurface.MCU_REPEAT, t::isLoop);
        this.addButton (ButtonID.STOP, "Stop", new StopCommand<> (this.model, surface), MCUControlSurface.MCU_STOP, () -> !t.isPlaying ());
        this.addButton (ButtonID.PLAY, "Play", new PlayCommand<> (this.model, surface), MCUControlSurface.MCU_PLAY, t::isPlaying);
        this.addButton (ButtonID.RECORD, "Record", new MCURecordCommand (this.model, surface), MCUControlSurface.MCU_RECORD, () -> {
            final boolean isOn = this.isRecordShifted (surface) ? t.isLauncherOverdub () : t.isRecording ();
            return isOn ? MCU_BUTTON_STATE_ON : MCU_BUTTON_STATE_OFF;
        });

        this.addButton (ButtonID.SCRUB, "Scrub", new ScrubCommand (this.model, surface), MCUControlSurface.MCU_SCRUB, () -> surface.getModeManager ().isActiveOrTempMode (Modes.DEVICE_PARAMS));
        this.addButton (ButtonID.ARROW_LEFT, "Left", new MCUCursorCommand (Direction.LEFT, this.model, surface), MCUControlSurface.MCU_ARROW_LEFT);
        this.addButton (ButtonID.ARROW_RIGHT, "Right", new MCUCursorCommand (Direction.RIGHT, this.model, surface), MCUControlSurface.MCU_ARROW_RIGHT);
        this.addButton (ButtonID.ARROW_UP, "Up", new MCUCursorCommand (Direction.UP, this.model, surface), MCUControlSurface.MCU_ARROW_UP);
        this.addButton (ButtonID.ARROW_DOWN, "Down", new MCUCursorCommand (Direction.DOWN, this.model, surface), MCUControlSurface.MCU_ARROW_DOWN);
        this.addButton (ButtonID.ZOOM, "Zoom", new ZoomCommand (this.model, surface), MCUControlSurface.MCU_ZOOM, surface.getConfiguration ()::isZoomState);

        // Display Mode
        this.addButton (ButtonID.TOGGLE_DISPLAY, "Toggle Display", new ToggleDisplayCommand (this.model, surface), MCUControlSurface.MCU_NAME_VALUE, surface.getConfiguration ()::isDisplayTrackNames);
        this.addButton (ButtonID.TEMPO_TICKS, "Tempo Ticks", new TempoTicksCommand (this.model, surface), MCUControlSurface.MCU_SMPTE_BEATS, this.configuration::isDisplayTicks);

        // Functions
        this.addButton (ButtonID.SHIFT, "Shift", new ShiftCommand<> (this.model, surface), MCUControlSurface.MCU_SHIFT);
        this.addButton (ButtonID.SELECT, "Option", NopCommand.INSTANCE, MCUControlSurface.MCU_OPTION);
        this.addButton (ButtonID.PUNCH_IN, "Punch In", new PunchInCommand<> (this.model, surface), MCUControlSurface.MCU_F6, t::isPunchInEnabled);
        this.addButton (ButtonID.PUNCH_OUT, "Punch Out", new PunchOutCommand<> (this.model, surface), MCUControlSurface.MCU_F7, t::isPunchOutEnabled);

        for (int i = 0; i < 5; i++)
        {
            final AssignableCommand command = new AssignableCommand (2 + i, this.model, surface);
            this.addButton (ButtonID.get (ButtonID.F1, i), "F" + (i + 1), command, MCUControlSurface.MCU_F1 + i, command::isActive);
        }

        final MoveTrackBankCommand<MCUControlSurface, MCUConfiguration> moveTrackBankLeftCommand = new MoveTrackBankCommand<> (this.model, surface, Modes.DEVICE_PARAMS, true, true);
        final MoveTrackBankCommand<MCUControlSurface, MCUConfiguration> moveTrackBankRightCommand = new MoveTrackBankCommand<> (this.model, surface, Modes.DEVICE_PARAMS, true, false);

        // Assignment - mode selection

        final ModeManager modeManager = surface.getModeManager ();

        this.addButton (ButtonID.TRACK, "Track", new TracksCommand (this.model, surface), MCUControlSurface.MCU_MODE_IO, () -> surface.getButton (ButtonID.SELECT).isPressed () ? this.model.isCursorTrackPinned () : modeManager.isActiveOrTempMode (Modes.TRACK, Modes.VOLUME));
        this.addButton (ButtonID.PAN_SEND, "Pan", new ModeSelectCommand<> (this.model, surface, Modes.PAN), MCUControlSurface.MCU_MODE_PAN, () -> modeManager.isActiveOrTempMode (Modes.PAN));
        this.addButton (ButtonID.SENDS, "Sends", new SendSelectCommand (this.model, surface), MCUControlSurface.MCU_MODE_SENDS, () -> Modes.isSendMode (modeManager.getActiveOrTempModeId ()));
        this.addButton (ButtonID.DEVICE, "Device", new DevicesCommand (this.model, surface), MCUControlSurface.MCU_MODE_PLUGIN, () -> surface.getButton (ButtonID.SELECT).isPressed () ? cursorDevice.isPinned () : modeManager.isActiveOrTempMode (Modes.DEVICE_PARAMS));
        this.addButton (ButtonID.MOVE_TRACK_LEFT, "Left", moveTrackBankLeftCommand, MCUControlSurface.MCU_MODE_EQ);
        this.addButton (ButtonID.MOVE_TRACK_RIGHT, "Right", moveTrackBankRightCommand, MCUControlSurface.MCU_MODE_DYN);

        // Automation
        this.addButton (ButtonID.AUTOMATION_TRIM, "Trim", new AutomationCommand<> (AutomationMode.TRIM_READ, this.model, surface), MCUControlSurface.MCU_TRIM, () -> !t.isWritingArrangerAutomation () && t.getAutomationWriteMode () == AutomationMode.TRIM_READ);
        this.addButton (ButtonID.AUTOMATION_READ, "Read", new AutomationCommand<> (AutomationMode.READ, this.model, surface), MCUControlSurface.MCU_READ, () -> !t.isWritingArrangerAutomation () && t.getAutomationWriteMode () != AutomationMode.TRIM_READ);
        this.addButton (ButtonID.AUTOMATION_WRITE, "Write", new AutomationCommand<> (AutomationMode.WRITE, this.model, surface), MCUControlSurface.MCU_WRITE, () -> t.isWritingArrangerAutomation () && t.getAutomationWriteMode () == AutomationMode.WRITE);
        this.addButton (ButtonID.AUTOMATION_GROUP, "Group/Write", new AutomationCommand<> (AutomationMode.LATCH_PREVIEW, this.model, surface), MCUControlSurface.MCU_GROUP, () -> t.isWritingArrangerAutomation () && t.getAutomationWriteMode () == AutomationMode.LATCH_PREVIEW);
        this.addButton (ButtonID.AUTOMATION_TOUCH, "Touch", new AutomationCommand<> (AutomationMode.TOUCH, this.model, surface), MCUControlSurface.MCU_TOUCH, () -> t.isWritingArrangerAutomation () && t.getAutomationWriteMode () == AutomationMode.TOUCH);
        this.addButton (ButtonID.AUTOMATION_LATCH, "Latch", new AutomationCommand<> (AutomationMode.LATCH, this.model, surface), MCUControlSurface.MCU_LATCH, () -> t.isWritingArrangerAutomation () && t.getAutomationWriteMode () == AutomationMode.LATCH);
        this.addButton (ButtonID.UNDO, "Undo", new UndoCommand<> (this.model, surface), MCUControlSurface.MCU_UNDO);

        // Panes
        this.addButton (ButtonID.NOTE_EDITOR, "Note Editor", new PaneCommand<> (PaneCommand.Panels.NOTE, this.model, surface), MCUControlSurface.MCU_MIDI_TRACKS);
        this.addButton (ButtonID.AUTOMATION_EDITOR, "Automation Editor", new PaneCommand<> (PaneCommand.Panels.AUTOMATION, this.model, surface), MCUControlSurface.MCU_INPUTS);
        this.addButton (ButtonID.TOGGLE_DEVICE, "Toggle Device", new PanelLayoutCommand<> (this.model, surface), MCUControlSurface.MCU_AUDIO_TRACKS, () -> !surface.isShiftPressed () && cursorDevice.isWindowOpen ());
        this.addButton (ButtonID.MIXER, "Mixer", new PaneCommand<> (PaneCommand.Panels.MIXER, this.model, surface), MCUControlSurface.MCU_AUDIO_INSTR);

        // Layouts
        this.addButton (ButtonID.LAYOUT_ARRANGE, "Arrange", new LayoutCommand<> (IApplication.PANEL_LAYOUT_ARRANGE, this.model, surface), MCUControlSurface.MCU_AUX);
        this.addButton (ButtonID.LAYOUT_MIX, "Mix", new LayoutCommand<> (IApplication.PANEL_LAYOUT_MIX, this.model, surface), MCUControlSurface.MCU_BUSSES);
        this.addButton (ButtonID.LAYOUT_EDIT, "Edit", new LayoutCommand<> (IApplication.PANEL_LAYOUT_EDIT, this.model, surface), MCUControlSurface.MCU_OUTPUTS);

        // Utilities
        this.addButton (ButtonID.BROWSE, "Browse", new BrowserCommand<> (Modes.BROWSER, this.model, surface), MCUControlSurface.MCU_USER, () -> modeManager.isActiveOrTempMode (Modes.BROWSER));
        this.addButton (ButtonID.METRONOME, "Metronome", new MetronomeCommand<> (this.model, surface), MCUControlSurface.MCU_CLICK, () -> surface.getButton (ButtonID.SHIFT).isPressed () ? t.isMetronomeTicksOn () : t.isMetronomeOn ());
        this.addButton (ButtonID.GROOVE, "Groove", new GrooveCommand (this.model, surface), MCUControlSurface.MCU_SOLO, () -> this.model.getGroove ().getParameters ()[0].getValue () > 0);
        this.addButton (ButtonID.OVERDUB, "Overdub", new OverdubCommand (this.model, surface), MCUControlSurface.MCU_REPLACE, () -> (surface.getButton (ButtonID.SHIFT).isPressed () ? t.isLauncherOverdub () : t.isArrangerOverdub ()));
        this.addButton (ButtonID.TAP_TEMPO, "Tap Tempo", new TapTempoCommand<> (this.model, surface), MCUControlSurface.MCU_NUDGE);
        this.addButton (ButtonID.DUPLICATE, "Duplicate", new DuplicateCommand<> (this.model, surface), MCUControlSurface.MCU_DROP);

        this.addButton (ButtonID.DEVICE_ON_OFF, "Device On/Off", new DeviceOnOffCommand<> (this.model, surface), MCUControlSurface.MCU_F8);

        // Currently not used but prevent error in console
        this.addButton (ButtonID.CONTROL, "Control", NopCommand.INSTANCE, MCUControlSurface.MCU_CONTROL);
        this.addButton (ButtonID.ALT, "Alt", NopCommand.INSTANCE, MCUControlSurface.MCU_ALT);

        // Fader Controls
        this.addButton (ButtonID.FLIP, "Flip", new ToggleTrackBanksCommand<> (this.model, surface), MCUControlSurface.MCU_FLIP, this.model::isEffectTrackBankActive);
        this.addButton (ButtonID.CANCEL, "Cancel", new KeyCommand (Key.ESCAPE, this.model, surface), MCUControlSurface.MCU_CANCEL);
        this.addButton (ButtonID.ENTER, "Enter", new KeyCommand (Key.ENTER, this.model, surface), MCUControlSurface.MCU_ENTER);

        this.addButton (ButtonID.MOVE_BANK_LEFT, "Bank Left", new MoveTrackBankCommand<> (this.model, surface, Modes.DEVICE_PARAMS, false, true), MCUControlSurface.MCU_BANK_LEFT);
        this.addButton (ButtonID.MOVE_BANK_RIGHT, "Bank Right", new MoveTrackBankCommand<> (this.model, surface, Modes.DEVICE_PARAMS, false, false), MCUControlSurface.MCU_BANK_RIGHT);
        surface.getButton (ButtonID.MOVE_TRACK_LEFT).bind (surface.getMidiInput (), this.getTriggerBindType (null), MCUControlSurface.MCU_TRACK_LEFT);
        surface.getButton (ButtonID.MOVE_TRACK_RIGHT).bind (surface.getMidiInput (), this.getTriggerBindType (null), MCUControlSurface.MCU_TRACK_RIGHT);

        // Additional command for footcontrollers
        this.addButton (ButtonID.NEW, "New", new NewCommand<> (this.model, surface), -1);

        // Only MCU
        this.addButton (ButtonID.SAVE, "Save", new SaveCommand<> (this.model, surface), MCUControlSurface.MCU_SAVE);
        this.addButton (ButtonID.MARKER, "Marker", new MarkerCommand<> (this.model, surface), MCUControlSurface.MCU_MARKER, () -> surface.getButton (ButtonID.SHIFT).isPressed () ? this.model.getArranger ().areCueMarkersVisible () : modeManager.isActiveOrTempMode (Modes.MARKERS));
        this.addButton (ButtonID.TOGGLE_VU, "Toggle VU", new ToggleVUCommand<> (this.model, surface), MCUControlSurface.MCU_EDIT, () -> this.configuration.isEnableVUMeters ());

        this.addLight (surface, OutputID.LED1, 0, MCUControlSurface.MCU_SMPTE_LED, () -> this.configuration.isDisplayTicks () ? 2 : 0);
        this.addLight (surface, OutputID.LED2, 0, MCUControlSurface.MCU_BEATS_LED, () -> !this.configuration.isDisplayTicks () ? 2 : 0);

        this.registerTriggerCommandsToAllDevices ();
    }


    /**
     * Common track editing - Assignment to all devices
     */
    protected void registerTriggerCommandsToAllDevices ()
    {
        for (int index = 0; index < this.numMCUDevices; index++)
        {
            final MCUControlSurface surface = this.getSurface (index);
            for (int i = 0; i < 8; i++)
            {
                final ButtonID row1ButtonID = ButtonID.get (ButtonID.ROW2_1, i);
                final ButtonID row2ButtonID = ButtonID.get (ButtonID.ROW3_1, i);
                final ButtonID row3ButtonID = ButtonID.get (ButtonID.ROW4_1, i);
                final ButtonID row4ButtonID = ButtonID.get (ButtonID.ROW_SELECT_1, i);

                final int labelIndex = 8 * (this.numMCUDevices - index - 1) + i + 1;

                this.addButton (surface, row1ButtonID, "Rec Arm " + labelIndex, new ButtonRowModeCommand<> (1, i, this.model, surface), MCUControlSurface.MCU_ARM1 + i, () -> getButtonColor (surface, row1ButtonID));
                this.addButton (surface, row2ButtonID, "Solo " + labelIndex, new ButtonRowModeCommand<> (2, i, this.model, surface), MCUControlSurface.MCU_SOLO1 + i, () -> getButtonColor (surface, row2ButtonID));
                this.addButton (surface, row3ButtonID, "Mute " + labelIndex, new ButtonRowModeCommand<> (3, i, this.model, surface), MCUControlSurface.MCU_MUTE1 + i, () -> getButtonColor (surface, row3ButtonID));
                this.addButton (surface, row4ButtonID, "Select " + labelIndex, new SelectCommand (i, this.model, surface), MCUControlSurface.MCU_SELECT1 + i, () -> getButtonColor (surface, row4ButtonID));
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void registerContinuousCommands ()
    {
        MCUControlSurface surface = this.getSurface ();
        IMidiInput input = surface.getMidiInput ();

        this.addRelativeKnob (ContinuousID.PLAY_POSITION, "Jog Wheel", new PlayPositionTempoCommand (this.model, surface), MCUControlSurface.MCU_CC_JOG, RelativeEncoding.SIGNED_BIT);

        final IHwFader master = this.addFader (ContinuousID.FADER_MASTER, "Master", new PitchbendVolumeCommand (8, this.model, surface), 8);
        master.bindTouch (new FaderTouchCommand (8, this.model, surface), input, BindType.NOTE, 0, MCUControlSurface.MCU_FADER_MASTER);

        for (int index = 0; index < this.numMCUDevices; index++)
        {
            surface = this.getSurface (index);
            input = surface.getMidiInput ();

            for (int i = 0; i < 8; i++)
            {
                final IHwRelativeKnob knob = this.addRelativeKnob (surface, ContinuousID.get (ContinuousID.KNOB1, i), "Knob " + i, new KnobRowModeCommand<> (i, this.model, surface), MCUControlSurface.MCU_CC_VPOT1 + i, RelativeEncoding.SIGNED_BIT);
                // Note: this is pressing the knobs' button not touching it!
                knob.bindTouch (new ButtonRowModeCommand<> (0, i, this.model, surface), input, BindType.NOTE, 0, MCUControlSurface.MCU_VSELECT1 + i);

                final IHwFader fader = this.addFader (surface, ContinuousID.get (ContinuousID.FADER1, i), "Fader " + (i + 1), new PitchbendVolumeCommand (i, this.model, surface), i);
                fader.bindTouch (new FaderTouchCommand (i, this.model, surface), input, BindType.NOTE, 0, MCUControlSurface.MCU_FADER_TOUCH1 + i);
            }
        }
    }


    /** {@inheritDoc} */
    @Override
    protected void layoutControls ()
    {
        MCUControlSurface surface = this.getSurface ();

        surface.getButton (ButtonID.FOOTSWITCH1).setBounds (12.5, 942.0, 77.75, 39.75);
        surface.getButton (ButtonID.FOOTSWITCH2).setBounds (102.5, 942.0, 77.75, 39.75);
        surface.getButton (ButtonID.REWIND).setBounds (556.0, 942.0, 65.0, 39.75);
        surface.getButton (ButtonID.FORWARD).setBounds (630.0, 942.0, 65.0, 39.75);
        surface.getButton (ButtonID.LOOP).setBounds (706.25, 942.0, 65.0, 39.75);
        surface.getButton (ButtonID.STOP).setBounds (779.0, 942.0, 65.0, 39.75);
        surface.getButton (ButtonID.PLAY).setBounds (850.25, 942.0, 65.0, 39.75);
        surface.getButton (ButtonID.RECORD).setBounds (923.5, 942.0, 65.0, 39.75);
        surface.getButton (ButtonID.SCRUB).setBounds (922.25, 697.25, 65.0, 39.75);
        surface.getButton (ButtonID.ARROW_LEFT).setBounds (705.0, 750.25, 65.0, 39.75);
        surface.getButton (ButtonID.ARROW_RIGHT).setBounds (849.0, 750.25, 65.0, 39.75);
        surface.getButton (ButtonID.ARROW_UP).setBounds (777.75, 702.25, 65.0, 39.75);
        surface.getButton (ButtonID.ARROW_DOWN).setBounds (777.75, 797.0, 65.0, 39.75);
        surface.getButton (ButtonID.ZOOM).setBounds (777.75, 750.25, 65.0, 39.75);
        surface.getButton (ButtonID.TOGGLE_DISPLAY).setBounds (701.25, 92.5, 77.75, 39.75);
        surface.getButton (ButtonID.TEMPO_TICKS).setBounds (879.0, 92.5, 77.75, 39.75);
        surface.getButton (ButtonID.SHIFT).setBounds (776.5, 637.5, 65.0, 39.75);
        surface.getButton (ButtonID.SELECT).setBounds (703.75, 637.5, 65.0, 39.75);
        surface.getButton (ButtonID.PUNCH_IN).setBounds (705.0, 320.25, 65.0, 39.75);
        surface.getButton (ButtonID.PUNCH_OUT).setBounds (777.75, 320.25, 65.0, 39.75);
        surface.getButton (ButtonID.F1).setBounds (632.5, 178.25, 65.0, 39.75);
        surface.getButton (ButtonID.F2).setBounds (705.0, 178.25, 65.0, 39.75);
        surface.getButton (ButtonID.F3).setBounds (777.75, 178.25, 65.0, 39.75);
        surface.getButton (ButtonID.F4).setBounds (849.25, 178.25, 65.0, 39.75);
        surface.getButton (ButtonID.F5).setBounds (921.5, 178.25, 65.0, 39.75);
        surface.getButton (ButtonID.TRACK).setBounds (705.0, 225.25, 65.0, 39.75);
        surface.getButton (ButtonID.PAN_SEND).setBounds (777.75, 225.25, 65.0, 39.75);
        surface.getButton (ButtonID.SENDS).setBounds (849.25, 225.25, 65.0, 39.75);
        surface.getButton (ButtonID.DEVICE).setBounds (921.5, 225.25, 65.0, 39.75);
        surface.getButton (ButtonID.MOVE_TRACK_LEFT).setBounds (847.75, 542.0, 65.0, 39.75);
        surface.getButton (ButtonID.MOVE_TRACK_RIGHT).setBounds (921.0, 542.0, 65.0, 39.75);
        surface.getButton (ButtonID.AUTOMATION_READ).setBounds (632.5, 272.25, 65.0, 39.75);
        surface.getButton (ButtonID.AUTOMATION_WRITE).setBounds (705.0, 272.25, 30.0, 39.75);
        surface.getButton (ButtonID.AUTOMATION_GROUP).setBounds (740.0, 272.25, 30.0, 39.75);
        surface.getButton (ButtonID.AUTOMATION_TRIM).setBounds (777.75, 272.25, 65.0, 39.75);
        surface.getButton (ButtonID.AUTOMATION_TOUCH).setBounds (849.25, 272.25, 65.0, 39.75);
        surface.getButton (ButtonID.AUTOMATION_LATCH).setBounds (921.5, 272.25, 65.0, 39.75);
        surface.getButton (ButtonID.UNDO).setBounds (849.25, 320.25, 65.0, 39.75);
        surface.getButton (ButtonID.NOTE_EDITOR).setBounds (705.0, 366.75, 65.0, 39.75);
        surface.getButton (ButtonID.AUTOMATION_EDITOR).setBounds (777.75, 366.75, 65.0, 39.75);
        surface.getButton (ButtonID.TOGGLE_DEVICE).setBounds (849.25, 366.75, 65.0, 39.75);
        surface.getButton (ButtonID.MIXER).setBounds (921.5, 366.75, 65.0, 39.75);
        surface.getButton (ButtonID.LAYOUT_ARRANGE).setBounds (703.75, 591.0, 65.0, 39.75);
        surface.getButton (ButtonID.LAYOUT_MIX).setBounds (776.5, 591.0, 65.0, 39.75);
        surface.getButton (ButtonID.LAYOUT_EDIT).setBounds (847.75, 591.0, 65.0, 39.75);
        surface.getButton (ButtonID.BROWSE).setBounds (705.0, 416.75, 65.0, 39.75);
        surface.getButton (ButtonID.METRONOME).setBounds (777.75, 416.75, 65.0, 39.75);
        surface.getButton (ButtonID.GROOVE).setBounds (849.25, 416.75, 65.0, 39.75);
        surface.getButton (ButtonID.OVERDUB).setBounds (921.5, 416.75, 65.0, 39.75);
        surface.getButton (ButtonID.TAP_TEMPO).setBounds (481.25, 942.0, 65.0, 39.75);
        surface.getButton (ButtonID.DUPLICATE).setBounds (776.5, 492.75, 65.0, 39.75);
        surface.getButton (ButtonID.DEVICE_ON_OFF).setBounds (921.0, 590.0, 65.0, 39.75);
        surface.getButton (ButtonID.CONTROL).setBounds (921.0, 637.5, 65.0, 39.75);
        surface.getButton (ButtonID.ALT).setBounds (847.75, 637.5, 65.0, 39.75);
        surface.getButton (ButtonID.FLIP).setBounds (703.75, 492.75, 65.0, 39.75);
        surface.getButton (ButtonID.CANCEL).setBounds (847.75, 492.75, 65.0, 39.75);
        surface.getButton (ButtonID.ENTER).setBounds (921.0, 492.75, 65.0, 39.75);
        surface.getButton (ButtonID.MOVE_BANK_LEFT).setBounds (703.75, 542.0, 65.0, 39.75);
        surface.getButton (ButtonID.MOVE_BANK_RIGHT).setBounds (776.5, 542.0, 65.0, 39.75);
        surface.getButton (ButtonID.NEW).setBounds (392.0, 942.0, 77.75, 39.75);
        surface.getButton (ButtonID.SAVE).setBounds (921.5, 320.25, 65.0, 39.75);
        surface.getButton (ButtonID.MARKER).setBounds (632.5, 225.25, 65.0, 39.75);
        surface.getButton (ButtonID.TOGGLE_VU).setBounds (790.25, 92.5, 77.75, 39.75);

        surface.getContinuous (ContinuousID.PLAY_POSITION).setBounds (859.5, 806.5, 115.25, 115.75);
        surface.getContinuous (ContinuousID.FADER_MASTER).setBounds (613.5, 501.5, 65.0, 419.0);

        surface.getTextDisplay (2).getHardwareDisplay ().setBounds (699.0, 27.0, 263.25, 44.25);
        surface.getTextDisplay (3).getHardwareDisplay ().setBounds (633.5, 92.5, 49.5, 39.75);

        surface.getLight (OutputID.LED1).setBounds (968.5, 92.5, 21.25, 12.75);
        surface.getLight (OutputID.LED2).setBounds (968.5, 119.5, 21.25, 12.75);

        for (int index = 0; index < this.numMCUDevices; index++)
        {
            surface = this.getSurface (index);

            surface.getButton (ButtonID.ROW2_1).setBounds (12.75, 178.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW3_1).setBounds (12.75, 225.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW4_1).setBounds (12.75, 272.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW_SELECT_1).setBounds (12.75, 366.75, 65.0, 39.75);
            surface.getButton (ButtonID.ROW2_2).setBounds (87.25, 178.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW3_2).setBounds (87.25, 225.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW4_2).setBounds (87.25, 272.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW_SELECT_2).setBounds (87.25, 366.75, 65.0, 39.75);
            surface.getButton (ButtonID.ROW2_3).setBounds (163.75, 178.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW3_3).setBounds (163.75, 225.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW4_3).setBounds (163.75, 272.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW_SELECT_3).setBounds (163.75, 366.75, 65.0, 39.75);
            surface.getButton (ButtonID.ROW2_4).setBounds (237.0, 178.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW3_4).setBounds (237.0, 225.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW4_4).setBounds (237.0, 272.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW_SELECT_4).setBounds (237.0, 366.75, 65.0, 39.75);
            surface.getButton (ButtonID.ROW2_5).setBounds (311.25, 178.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW3_5).setBounds (311.25, 225.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW4_5).setBounds (311.25, 272.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW_SELECT_5).setBounds (311.25, 366.75, 65.0, 39.75);
            surface.getButton (ButtonID.ROW2_6).setBounds (386.5, 178.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW3_6).setBounds (386.5, 225.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW4_6).setBounds (386.5, 272.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW_SELECT_6).setBounds (386.5, 366.75, 65.0, 39.75);
            surface.getButton (ButtonID.ROW2_7).setBounds (459.0, 178.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW3_7).setBounds (459.0, 225.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW4_7).setBounds (459.0, 272.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW_SELECT_7).setBounds (459.0, 366.75, 65.0, 39.75);
            surface.getButton (ButtonID.ROW2_8).setBounds (532.25, 178.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW3_8).setBounds (532.25, 225.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW4_8).setBounds (532.25, 272.25, 65.0, 39.75);
            surface.getButton (ButtonID.ROW_SELECT_8).setBounds (532.25, 366.75, 65.0, 39.75);

            surface.getContinuous (ContinuousID.KNOB1).setBounds (12.25, 93.5, 72.0, 74.75);
            surface.getContinuous (ContinuousID.KNOB2).setBounds (86.25, 93.5, 72.0, 74.75);
            surface.getContinuous (ContinuousID.KNOB3).setBounds (160.0, 93.5, 72.0, 74.75);
            surface.getContinuous (ContinuousID.KNOB4).setBounds (234.0, 93.5, 72.0, 74.75);
            surface.getContinuous (ContinuousID.KNOB5).setBounds (308.0, 93.5, 72.0, 74.75);
            surface.getContinuous (ContinuousID.KNOB6).setBounds (381.75, 93.5, 72.0, 74.75);
            surface.getContinuous (ContinuousID.KNOB7).setBounds (455.75, 93.5, 72.0, 74.75);
            surface.getContinuous (ContinuousID.KNOB8).setBounds (529.5, 93.5, 72.0, 74.75);
            surface.getContinuous (ContinuousID.FADER1).setBounds (12.5, 501.5, 65.0, 419.0);
            surface.getContinuous (ContinuousID.FADER2).setBounds (87.25, 501.5, 65.0, 419.0);
            surface.getContinuous (ContinuousID.FADER3).setBounds (163.75, 501.5, 65.0, 419.0);
            surface.getContinuous (ContinuousID.FADER4).setBounds (237.0, 501.5, 65.0, 419.0);
            surface.getContinuous (ContinuousID.FADER5).setBounds (311.25, 501.5, 65.0, 419.0);
            surface.getContinuous (ContinuousID.FADER6).setBounds (386.5, 499.5, 65.0, 419.0);
            surface.getContinuous (ContinuousID.FADER7).setBounds (459.0, 501.5, 65.0, 419.0);
            surface.getContinuous (ContinuousID.FADER8).setBounds (532.25, 501.5, 65.0, 419.0);

            surface.getTextDisplay (0).getHardwareDisplay ().setBounds (11.75, 11.75, 601.0, 73.25);
            surface.getTextDisplay (1).getHardwareDisplay ().setBounds (11.5, 419.5, 668.25, 73.25);
        }
    }


    /** {@inheritDoc} */
    @Override
    public void startup ()
    {
        for (int index = 0; index < this.numMCUDevices; index++)
        {
            final MCUControlSurface surface = this.getSurface (index);
            surface.switchVuMode (MCUControlSurface.VUMODE_LED);

            surface.getViewManager ().setActiveView (Views.CONTROL);
            surface.getModeManager ().setActiveMode (Modes.PAN);
        }
    }


    private void updateSegmentDisplay ()
    {
        if (!this.configuration.hasSegmentDisplay ())
            return;

        final ITransport t = this.model.getTransport ();

        String positionText = this.configuration.isDisplayTime () ? t.getPositionText () : t.getBeatText ();
        positionText = positionText.replace ('.', ':');

        if (this.configuration.isDisplayTicks ())
        {
            if (!this.configuration.isDisplayTime ())
                positionText += " ";
        }
        else
        {
            String tempoStr = t.formatTempoNoFraction (t.getTempo ());
            final int pos = positionText.lastIndexOf (':');
            if (tempoStr.length () < 3)
                tempoStr = "0" + tempoStr;
            positionText = positionText.substring (0, pos + 1) + tempoStr;
        }

        this.getSurface ().getTextDisplay (2).setRow (0, positionText).allDone ();
    }


    private void updateVUMeters ()
    {
        if (!this.configuration.isEnableVUMeters ())
            return;

        final double upperBound = this.valueChanger.getUpperBound ();

        final ITrackBank tb = this.model.getCurrentTrackBank ();
        for (int index = 0; index < this.numMCUDevices; index++)
        {
            final MCUControlSurface surface = this.getSurface (index);
            final IMidiOutput output = surface.getMidiOutput ();
            final int extenderOffset = surface.getExtenderOffset ();
            for (int i = 0; i < 8; i++)
            {
                final int channel = extenderOffset + i;
                final ITrack track = tb.getItem (channel);

                final int vu = track.getVu ();
                if (vu != this.vuValues[channel])
                {
                    this.vuValues[channel] = vu;
                    final int scaledValue = (int) Math.round (vu * 12 / upperBound);
                    output.sendChannelAftertouch (0x10 * i + scaledValue, 0);
                }
            }
        }

        final IMasterTrack masterTrack = this.model.getMasterTrack ();
        final IMidiOutput output = this.getSurface ().getMidiOutput ();

        // Stereo VU of master channel
        int vu = masterTrack.getVuLeft ();
        if (vu != this.masterVuValues[0])
        {
            this.masterVuValues[0] = vu;
            final int scaledValue = (int) Math.round (vu * 12 / upperBound);
            output.sendChannelAftertouch (1, scaledValue, 0);
        }

        vu = masterTrack.getVuRight ();
        if (vu != this.masterVuValues[1])
        {
            this.masterVuValues[1] = vu;
            final int scaledValue = (int) Math.round (vu * 12 / upperBound);
            output.sendChannelAftertouch (1, 0x10 + scaledValue, 0);
        }
    }


    private void updateFaders (final boolean isShiftPressed)
    {
        if (!this.configuration.hasMotorFaders ())
            return;

        final Modes activeMode = this.getSurface ().getModeManager ().getActiveOrTempModeId ();
        final Modes modeId = this.configuration.useFadersAsKnobs () && VALUE_MODES.contains (activeMode) ? activeMode : Modes.VOLUME;

        for (int surfaceIndex = 0; surfaceIndex < this.numMCUDevices; surfaceIndex++)
        {
            final MCUControlSurface surface = this.getSurface (surfaceIndex);
            final Mode mode = surface.getModeManager ().getMode (modeId);
            final IMidiOutput output = surface.getMidiOutput ();
            for (int channel = 0; channel < 8; channel++)
            {
                // Dont't update fader if the user touches and therefore 'stops' it
                if (mode.isKnobTouched (channel))
                    continue;

                final int value = mode.getKnobValue (channel);
                final int position = surface.getExtenderOffset () + channel;
                if (value != this.faderValues[position])
                {
                    this.faderValues[position] = value;
                    output.sendPitchbend (channel, value % 127, value / 127);
                }
            }
        }

        // Update motor fader of master channel
        final int volume = isShiftPressed ? this.model.getTransport ().getMetronomeVolume () : this.model.getMasterTrack ().getVolume ();
        if (volume != this.masterFaderValue)
        {
            this.masterFaderValue = volume;
            this.getSurface ().getMidiOutput ().sendPitchbend (8, volume % 127, volume / 127);
        }
    }


    private void updateMode (final Modes mode)
    {
        if (mode == null)
            return;

        this.updateIndication (mode);
        if (this.configuration.hasAssignmentDisplay ())
            this.getSurface ().getTextDisplay (3).setRow (0, MODE_ACRONYMS.get (mode)).allDone ();
    }


    /** {@inheritDoc} */
    @Override
    protected void updateIndication (final Modes mode)
    {
        if (this.currentMode != null && this.currentMode.equals (mode))
            return;
        this.currentMode = mode;

        final ITrackBank tb = this.model.getTrackBank ();
        final ITrackBank tbe = this.model.getEffectTrackBank ();
        final boolean isEffect = this.model.isEffectTrackBankActive ();
        final boolean isPan = Modes.PAN.equals (mode);
        final boolean isTrack = Modes.TRACK.equals (mode);
        final boolean isVolume = Modes.VOLUME.equals (mode);
        final boolean isDevice = Modes.DEVICE_PARAMS.equals (mode);

        tb.setIndication (!isEffect);
        if (tbe != null)
            tbe.setIndication (isEffect);

        final ICursorDevice cursorDevice = this.model.getCursorDevice ();
        final ITrack selectedTrack = tb.getSelectedItem ();
        for (int i = 0; i < tb.getPageSize (); i++)
        {
            final boolean hasTrackSel = selectedTrack != null && selectedTrack.getIndex () == i && isTrack;
            final ITrack track = tb.getItem (i);
            track.setVolumeIndication (!isEffect && (isVolume || hasTrackSel));
            track.setPanIndication (!isEffect && (isPan || hasTrackSel));

            final ISendBank sendBank = track.getSendBank ();
            for (int j = 0; j < sendBank.getPageSize (); j++)
                sendBank.getItem (j).setIndication (!isEffect && (mode.ordinal () - Modes.SEND1.ordinal () == j || hasTrackSel));

            if (tbe != null)
            {
                final ITrack fxTrack = tbe.getItem (i);
                fxTrack.setVolumeIndication (isEffect);
                fxTrack.setPanIndication (isEffect && isPan);
            }
        }

        final IParameterBank parameterBank = cursorDevice.getParameterBank ();
        for (int i = 0; i < parameterBank.getPageSize (); i++)
            parameterBank.getItem (i).setIndication (isDevice);
    }


    /** {@inheritDoc} */
    @Override
    protected BindType getTriggerBindType (final ButtonID buttonID)
    {
        return BindType.NOTE;
    }


    /**
     * Handle a track selection change.
     *
     * @param isSelected Has the track been selected?
     */
    private void handleTrackChange (final boolean isSelected)
    {
        if (!isSelected)
            return;

        final ModeManager modeManager = this.getSurface ().getModeManager ();
        if (modeManager.isActiveOrTempMode (Modes.MASTER))
            modeManager.setActiveMode (Modes.TRACK);
    }


    private static int getButtonColor (final MCUControlSurface surface, final ButtonID buttonID)
    {
        final Mode mode = surface.getModeManager ().getActiveOrTempMode ();
        return mode == null ? 0 : mode.getButtonColor (buttonID);
    }
}
