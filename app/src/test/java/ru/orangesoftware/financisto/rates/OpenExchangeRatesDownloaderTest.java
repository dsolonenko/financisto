package ru.orangesoftware.financisto.rates;

import org.junit.Test;

import java.util.List;

import ru.orangesoftware.financisto.utils.FileUtils;

import static org.junit.Assert.*;

public class OpenExchangeRatesDownloaderTest extends AbstractRatesDownloaderTest {

    OpenExchangeRatesDownloader openRates = new OpenExchangeRatesDownloader(client, "MY_APP_ID");

    @Override
    ExchangeRateProvider service() {
        return openRates;
    }

    @Test
    public void should_download_single_rate_usd_to_cur() {
        //given
        givenResponseFromWebService("http://openexchangerates.org/api/latest.json?app_id=MY_APP_ID",
                "open_exchange_normal_response.json");
        //when
        ExchangeRate downloadedExchangeRate = downloadRate("USD", "SGD");
        //then
        assertTrue(downloadedExchangeRate.isOk());
        assertEquals(1.236699, downloadedExchangeRate.rate, 0.00001);
        assertEquals(1361034009000L, downloadedExchangeRate.date);
    }

    @Test
    public void should_download_single_rate_cur_to_cur() {
        //given
        givenResponseFromWebService(anyUrl(), "open_exchange_normal_response.json");
        //then
        assertEquals(1.0 / 1.236699, downloadRate("SGD", "USD").rate, 0.00001);
        assertEquals(0.00010655, downloadRate("BYR", "CHF").rate, 0.00001);
    }

    @Test
    public void should_download_multiple_rates() {
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

    @Test
    public void should_skip_unknown_currency() {
        //given
        givenResponseFromWebService(anyUrl(), "open_exchange_normal_response.json");
        //when
        ExchangeRate rate = downloadRate("USD", "AAA");
        //then
        assertFalse(rate.isOk());
        assertRate(rate, "USD", "AAA");
    }

    @Test
    public void should_handle_error_from_webservice_properly() {
        //given
        givenResponseFromWebService(anyUrl(), "open_exchange_error_response.json");
        //when
        ExchangeRate downloadedRate = downloadRate("USD", "SGD");
        //then
        assertFalse(downloadedRate.isOk());
        assertRate(downloadedRate, "USD", "SGD");
        assertEquals("400 (invalid_app_id): Invalid App ID", downloadedRate.getErrorMessage());
    }

    @Test
    public void should_handle_runtime_error_properly() {
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
        super.givenResponseFromWebService(url, FileUtils.testFileAsString(fileName));
    }

}
