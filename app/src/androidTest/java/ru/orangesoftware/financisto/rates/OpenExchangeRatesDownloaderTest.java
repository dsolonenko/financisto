/*
 * Copyright (c) 2013 Denis Solonenko.
 * All rights reserved. This program and the accompanying materials
 * are made available under the terms of the GNU Public License v2.0
 * which accompanies this distribution, and is available at
 * http://www.gnu.org/licenses/old-licenses/gpl-2.0.html
 */

package ru.orangesoftware.financisto.rates;

import org.apache.http.entity.InputStreamEntity;
import org.apache.http.util.EntityUtils;

import java.io.InputStream;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: dsolonenko
 * Date: 2/16/13
 * Time: 6:28 PM
 */
public class OpenExchangeRatesDownloaderTest extends AbstractRatesDownloaderTest {

    OpenExchangeRatesDownloader openRates = new OpenExchangeRatesDownloader(client, "MY_APP_ID");

    @Override
    ExchangeRateProvider service() {
        return openRates;
    }

    public void test_should_download_single_rate_usd_to_cur() {
        //given
        givenResponseFromWebService("http://openexchangerates.org/api/latest.json?app_id=MY_APP_ID",
                "open_exchange_normal_response.json");
        //when
        ExchangeRate downloadedExchangeRate = downloadRate("USD", "SGD");
        //then
        assertTrue(downloadedExchangeRate.isOk());
        assertEquals(1.236699, downloadedExchangeRate.rate);
        assertEquals(1361034009000L, downloadedExchangeRate.date);
    }

    public void test_should_download_single_rate_cur_to_cur() {
        //given
        givenResponseFromWebService(anyUrl(), "open_exchange_normal_response.json");
        //then
        assertEquals(1.0 / 1.236699, downloadRate("SGD", "USD").rate);
        assertEquals(0.00010655, downloadRate("BYR", "CHF").rate, 0.00001);
    }

    public void test_should_download_multiple_rates() {
        //given
        givenResponseFromWebService(anyUrl(), "open_exchange_normal_response.json");
        //when
        List<ExchangeRate> rates = openRates.getRates(currencies("USD", "SGD", "RUB"));
        //then
        assertEquals(3, rates.size());
        assertRate(rates.get(0), "USD", "SGD", 1.236699, 1361034009000L);
        assertRate(rates.get(1), "USD", "RUB", 30.117065, 1361034009000L);
        assertRate(rates.get(2), "SGD", "RUB", 24.352785, 1361034009000L);
    }

    public void test_should_skip_unknown_currency() {
        //given
        givenResponseFromWebService(anyUrl(), "open_exchange_normal_response.json");
        //when
        ExchangeRate rate = downloadRate("USD", "AAA");
        //then
        assertFalse(rate.isOk());
        assertRate(rate, "USD", "AAA");
    }

    public void test_should_handle_error_from_webservice_properly() {
        //given
        givenResponseFromWebService(anyUrl(), "open_exchange_error_response.json");
        //when
        ExchangeRate downloadedRate = downloadRate("USD", "SGD");
        //then
        assertFalse(downloadedRate.isOk());
        assertRate(downloadedRate, "USD", "SGD");
        assertEquals("400 (invalid_app_id): Invalid App ID", downloadedRate.getErrorMessage());
    }

    public void test_should_handle_runtime_error_properly() {
        //given
        givenExceptionWhileRequestingWebService();
        //when
        ExchangeRate downloadedRate = downloadRate("USD", "SGD");
        //then
        assertFalse(downloadedRate.isOk());
        assertEquals("Unable to get exchange rates: Timeout", downloadedRate.getErrorMessage());
    }

    @Override
    void givenResponseFromWebService(String url, String fileName) {
        super.givenResponseFromWebService(url, fileAsString(fileName));
    }

    private String fileAsString(String fileName) {
        try {
            InputStream is = getInstrumentation().getContext().getResources().getAssets().open(fileName);
            InputStreamEntity entity = new InputStreamEntity(is, is.available());
            return EntityUtils.toString(entity);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

}
