
/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.service.impl;

import de.hybris.platform.catalog.model.CatalogUnawareMediaModel;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.servicelayer.exceptions.SystemException;
import de.hybris.platform.servicelayer.media.MediaService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;

import java.io.InputStream;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.cci.hybris.stripe.fulfilmentprocess.service.StripefulfilmentprocessService;


/**
 * Default fulfilment-process support service for Stripe email assets.
 */
public class DefaultStripefulfilmentprocessService implements StripefulfilmentprocessService
{
	private static final Logger LOG = LoggerFactory.getLogger(DefaultStripefulfilmentprocessService.class);
	private static final String FIND_LOGO_QUERY = "SELECT {" + CatalogUnawareMediaModel.PK + "} FROM {"
			+ CatalogUnawareMediaModel._TYPECODE + "} WHERE {" + CatalogUnawareMediaModel.CODE + "}=?code";
	private static final String FIND_LOGO_CODE_PARAM = "code";
	private static final String PLATFORM_LOGO_FILE_NAME = "sap-hybris-platform.png";
	private static final String PLATFORM_LOGO_RESOURCE_PATH = "/stripefulfilmentprocess/" + PLATFORM_LOGO_FILE_NAME;

	private MediaService mediaService;
	private ModelService modelService;
	private FlexibleSearchService flexibleSearchService;
	@Override
	public String getHybrisLogoUrl(final String logoCode)
	{
		final MediaModel media = getMediaService().getMedia(logoCode);

		// Keep in mind that with Slf4j you don't need to check if debug is enabled, it is done under the hood.
		LOG.debug("Found media [code: {}]", media.getCode());

		return media.getURL();
	}

	/**
	 * Creates the fulfilment-process logo media when it does not already exist.
	 */
	@Override
	public void createLogo(final String logoCode)
	{
		final Optional<CatalogUnawareMediaModel> existingLogo = findExistingLogo(logoCode);
		final CatalogUnawareMediaModel media = existingLogo
				.orElseGet(() -> getModelService().create(CatalogUnawareMediaModel.class));
		media.setCode(logoCode);
		media.setRealFileName(PLATFORM_LOGO_FILE_NAME);
		getModelService().save(media);

		getMediaService().setStreamForMedia(media, getImageStream());
	}

	private Optional<CatalogUnawareMediaModel> findExistingLogo(final String logoCode)
	{
		final FlexibleSearchQuery fQuery = new FlexibleSearchQuery(FIND_LOGO_QUERY);
		fQuery.addQueryParameter(FIND_LOGO_CODE_PARAM, logoCode);

		try
		{
			return Optional.of(getFlexibleSearchService().searchUnique(fQuery));
		}
		catch (final SystemException e)
		{
			return Optional.empty();
		}
	}

	private InputStream getImageStream()
	{
		return DefaultStripefulfilmentprocessService.class.getResourceAsStream(PLATFORM_LOGO_RESOURCE_PATH);
	}

	/**
	 * Sets the media service dependency.
	 *
	 * @param mediaService media service
	 */
	public void setMediaService(final MediaService mediaService)
	{
		this.mediaService = mediaService;
	}

	/**
	 * Sets the model service dependency.
	 *
	 * @param modelService model service
	 */
	public void setModelService(final ModelService modelService)
	{
		this.modelService = modelService;
	}

	/**
	 * Sets the flexible-search service dependency.
	 *
	 * @param flexibleSearchService flexible-search service
	 */
	public void setFlexibleSearchService(final FlexibleSearchService flexibleSearchService)
	{
		this.flexibleSearchService = flexibleSearchService;
	}
	protected MediaService getMediaService()
	{
		return mediaService;
	}
	protected ModelService getModelService()
	{
		return modelService;
	}
	protected FlexibleSearchService getFlexibleSearchService()
	{
		return flexibleSearchService;
	}
}
