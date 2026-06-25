/*
 * SPDX-License-Identifier: MIT
 * Copyright (c) 2026 Nina Labs AI
 */

package com.cci.hybris.stripe.fulfilmentprocess.service.impl;

import de.hybris.bootstrap.annotations.UnitTest;
import de.hybris.platform.catalog.model.CatalogUnawareMediaModel;
import de.hybris.platform.core.model.media.MediaModel;
import de.hybris.platform.servicelayer.exceptions.SystemException;
import de.hybris.platform.servicelayer.media.MediaService;
import de.hybris.platform.servicelayer.model.ModelService;
import de.hybris.platform.servicelayer.search.FlexibleSearchQuery;
import de.hybris.platform.servicelayer.search.FlexibleSearchService;
import org.junit.Before;
import org.junit.Test;
import org.mockito.ArgumentCaptor;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@UnitTest
public class DefaultStripefulfilmentprocessServiceTest
{
	private static final String LOGO_CODE = "stripe-logo";

	private ExposedDefaultStripefulfilmentprocessService testObj;
	private MediaService mediaService;
	private ModelService modelService;
	private FlexibleSearchService flexibleSearchService;

	@Before
	public void setUp()
	{
		testObj = new ExposedDefaultStripefulfilmentprocessService();
		mediaService = mock(MediaService.class);
		modelService = mock(ModelService.class);
		flexibleSearchService = mock(FlexibleSearchService.class);

		testObj.setMediaService(mediaService);
		testObj.setModelService(modelService);
		testObj.setFlexibleSearchService(flexibleSearchService);
	}

	@Test
	public void getHybrisLogoUrl_returnsResolvedMediaUrl()
	{
		final MediaModel media = mock(MediaModel.class);
		when(media.getCode()).thenReturn(LOGO_CODE);
		when(media.getURL()).thenReturn("/medias/logo.png");
		when(mediaService.getMedia(LOGO_CODE)).thenReturn(media);

		final String result = testObj.getHybrisLogoUrl(LOGO_CODE);

		assertEquals("/medias/logo.png", result);
		verify(mediaService).getMedia(LOGO_CODE);
	}

	@Test
	public void createLogo_existingLogo_reusesExistingModel()
	{
		final CatalogUnawareMediaModel existing = new CatalogUnawareMediaModel();
		when(flexibleSearchService.searchUnique(any(FlexibleSearchQuery.class))).thenReturn(existing);

		testObj.createLogo(LOGO_CODE);

		verify(modelService, never()).create(CatalogUnawareMediaModel.class);
		verify(modelService).save(existing);
		verify(mediaService).setStreamForMedia(eq(existing), any());
		assertEquals(LOGO_CODE, existing.getCode());
		assertEquals("sap-hybris-platform.png", existing.getRealFileName());
	}

	@Test
	public void createLogo_missingLogo_createsModelAndSetsQueryParameter()
	{
		final CatalogUnawareMediaModel created = new CatalogUnawareMediaModel();
		when(flexibleSearchService.searchUnique(any(FlexibleSearchQuery.class))).thenThrow(new SystemException("not-found"));
		when(modelService.create(CatalogUnawareMediaModel.class)).thenReturn(created);

		testObj.createLogo(LOGO_CODE);

		verify(modelService).create(CatalogUnawareMediaModel.class);
		verify(modelService).save(created);
		verify(mediaService).setStreamForMedia(eq(created), any());
		assertEquals(LOGO_CODE, created.getCode());
		assertEquals("sap-hybris-platform.png", created.getRealFileName());

		final ArgumentCaptor<FlexibleSearchQuery> queryCaptor = ArgumentCaptor.forClass(FlexibleSearchQuery.class);
		verify(flexibleSearchService).searchUnique(queryCaptor.capture());
		assertEquals(LOGO_CODE, queryCaptor.getValue().getQueryParameters().get("code"));
	}

	@Test
	public void protectedGetters_returnInjectedDependencies()
	{
		assertSame(mediaService, testObj.exposedMediaService());
		assertSame(modelService, testObj.exposedModelService());
		assertSame(flexibleSearchService, testObj.exposedFlexibleSearchService());
		assertNotNull(testObj.exposedImageStream());
	}

	private static class ExposedDefaultStripefulfilmentprocessService extends DefaultStripefulfilmentprocessService
	{
		private MediaService exposedMediaService()
		{
			return getMediaService();
		}

		private ModelService exposedModelService()
		{
			return getModelService();
		}

		private FlexibleSearchService exposedFlexibleSearchService()
		{
			return getFlexibleSearchService();
		}

		private java.io.InputStream exposedImageStream()
		{
			return ExposedDefaultStripefulfilmentprocessService.class
					.getResourceAsStream("/stripefulfilmentprocess/sap-hybris-platform.png");
		}
	}
}
