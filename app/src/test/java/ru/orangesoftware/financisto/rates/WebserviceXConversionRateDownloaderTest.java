package ru.orangesoftware.financisto.rates;

import org.junit.Test;

import java.util.List;

import static org.junit.Assert.*;

public class WebserviceXConversionRateDownloaderTest extends AbstractRatesDownloaderTest {

    long dateTime = System.currentTimeMillis();
    WebserviceXConversionRateDownloader webserviceX = new WebserviceXConversionRateDownloader(client, dateTime);

    @Override
    ExchangeRateProvider service() {
        return webserviceX;
    }

    @Test
    public void should_download_single_rate_cur_to_cur() {
        //given
        givenResponseFromWebService("USD", "SGD", 1.2387);
        //when
        ExchangeRate exchangeRate = downloadRate("USD", "SGD");
        //then
        assertEquals(1.2387, exchangeRate.rate, 0.0001);
    }

    @Test
    public void should_download_multiple_rates() {
        //given
        givenResponseFromWebService("USD", "SGD", 1.2);
        givenResponseFromWebService("USD", "RUB", 30);
        givenResponseFromWebService("SGD", "RUB", 25);
        //when
        List<ExchangeRate> rates = webserviceX.getRates(currencies("USD", "SGD", "RUB"));
        //then
        assertEquals(3, rates.size());
        assertRate(rates.get(0), "USD", "SGD", 1.2, dateTime);
        assertRate(rates.get(1), "USD", "RUB", 30, dateTime);
        assertRate(rates.get(2), "SGD", "RUB", 25, dateTime);
    }

    @Test
    public void should_skip_unknown_currency() {
        //given
        givenResponseFromWebService(anyUrl(), "Exception: Unable to convert ToCurrency to Currency\r\nStacktrace...");
        //when
        ExchangeRate rate = downloadRate("USD", "AAA");
        //then
        assertFalse(rate.isOk());
        assertRate(rate, "USD", "AAA");
    }

    @Test
    public void should_handle_error_from_webservice_properly() {
        //given
        givenResponseFromWebService(anyUrl(), "System.IO.IOException: There is not enough space on the disk.\r\nStacktrace...");
        //when
        ExchangeRate downloadedRate = downloadRate("USD", "SGD");
        //then
        assertFalse(downloadedRate.isOk());
        assertEquals("Something wrong with the exchange rates provider. Response from the service - System.IO.IOException: There is not enough space on the disk.",
                downloadedRate.getErrorMessage());
    }

    @Test
    public void should_handle_runtime_error_properly() {
        //given
        givenExceptionWhileRequestingWebService();
        //when
        ExchangeRate downloadedRate = downloadRate("USD", "SGD");
        //then
        assertFalse(downloadedRate.isOk());
        assertRate(downloadedRate, "USD", "SGD");
        assertEquals("Unable to get exchange rates: Timeout", downloadedRate.getErrorMessage());
    }

    private void givenResponseFromWebService(String c1, String c2, double r) {
        givenResponseFromWebService("http://www.webservicex.net/CurrencyConvertor.asmx/ConversionRate?FromCurrency=" + c1 + "&ToCurrency=" + c2,
                "<double xmlns=\"http://www.webserviceX.NET/\">" + r + "</double>");
    }

}
