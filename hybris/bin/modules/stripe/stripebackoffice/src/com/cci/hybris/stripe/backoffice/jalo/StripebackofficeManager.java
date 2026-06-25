package com.cci.hybris.stripe.backoffice.jalo;

import com.cci.hybris.stripe.backoffice.constants.StripebackofficeConstants;
import de.hybris.platform.jalo.JaloSession;
import de.hybris.platform.jalo.extension.ExtensionManager;
import org.apache.log4j.Logger;

public class StripebackofficeManager extends GeneratedStripebackofficeManager
{
	@SuppressWarnings("unused")
	private static final Logger log = Logger.getLogger( StripebackofficeManager.class.getName() );
	
	public static final StripebackofficeManager getInstance()
	{
		ExtensionManager em = JaloSession.getCurrentSession().getExtensionManager();
		return (StripebackofficeManager) em.getExtension(StripebackofficeConstants.EXTENSIONNAME);
	}
	
}
