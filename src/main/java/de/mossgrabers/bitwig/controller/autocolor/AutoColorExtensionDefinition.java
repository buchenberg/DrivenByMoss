// Written by Jürgen Moßgraber - mossgrabers.de
// (c) 2017-2020
// Licensed under LGPLv3 - http://www.gnu.org/licenses/lgpl-3.0.txt

package de.mossgrabers.bitwig.controller.autocolor;

import de.mossgrabers.bitwig.framework.BitwigSetupFactory;
import de.mossgrabers.bitwig.framework.configuration.SettingsUIImpl;
import de.mossgrabers.bitwig.framework.daw.HostImpl;
import de.mossgrabers.bitwig.framework.extension.AbstractControllerExtensionDefinition;
import de.mossgrabers.controller.autocolor.AutoColorDefinition;
import de.mossgrabers.controller.autocolor.AutoColorSetup;
import de.mossgrabers.framework.controller.IControllerSetup;

import com.bitwig.extension.controller.api.ControllerHost;


/**
 * Definition class for Auto Color.
 *
 * @author J&uuml;rgen Mo&szlig;graber
 */
public class AutoColorExtensionDefinition extends AbstractControllerExtensionDefinition
{
    /**
     * Constructor.
     */
    public AutoColorExtensionDefinition ()
    {
        super (new AutoColorDefinition ());
    }


    /** {@inheritDoc} */
    @Override
    protected IControllerSetup<?, ?> getControllerSetup (final ControllerHost host)
    {
        return new AutoColorSetup (new HostImpl (host), new BitwigSetupFactory (host), new SettingsUIImpl (host.getPreferences ()), new SettingsUIImpl (host.getDocumentState ()));
    }
}
