package rehanced.com.simpleetherwallet.network;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.drawable.BitmapDrawable;

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.concurrent.TimeUnit;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Protocol;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.ResponseBody;
import rehanced.com.simpleetherwallet.interfaces.LastIconLoaded;
import rehanced.com.simpleetherwallet.interfaces.StorableWallet;
import rehanced.com.simpleetherwallet.utils.RequestCache;
import rehanced.com.simpleetherwallet.utils.TokenIconCache;

public class EllaismNetwork implements NetworkAPI {
    private String token;
    private String apiUrl = "http://ellaismwallet.nonlocal.ca/";

    EllaismNetwork(String theToken)
    {
        token = theToken;
    }

    public void getPriceChart(long starttime, int period, boolean usd, Callback b) throws IOException {
        String url = null;
        switch (period) {
            case 300: url = apiUrl + "returnChartData?fsym=ELLA&tsym=" + (usd ? "USD" : "BTC") + "&period=histominute&limit=1440";
            break;
            case 1800: url = apiUrl + "returnChartData?fsym=ELLA&tsym=" + (usd ? "USD" : "BTC") + "&period=histohour&limit=168";
            break;
            case 14400: url = apiUrl + "returnChartData?fsym=ELLA&tsym=" + (usd ? "USD" : "BTC") + "&period=histoday&limit=30";
            break;
            default:
                url = apiUrl + "returnChartData?fsym=ELLA&tsym=" + (usd ? "USD" : "BTC") + "&period=histoday&limit=365";
        }

        get(url, b);
    }


    /**
     * Retrieve all internal transactions from address like contract calls, for normal transactions @see rehanced.com.simpleetherwallet.network.EtherscanAPI#getNormalTransactions() )
     *
     * @param address Ether address
     * @param b       Network callback to @see rehanced.com.simpleetherwallet.fragments.FragmentTransactions#update() or @see rehanced.com.simpleetherwallet.fragments.FragmentTransactionsAll#update()
     * @param force   Whether to force (true) a network call or use cache (false). Only true if user uses swiperefreshlayout
     * @throws IOException Network exceptions
     */
    public void getInternalTransactions(String address, Callback b, boolean force) throws IOException {
        String url = apiUrl + "txlistinternal?address=" + address + "&startblock=0&endblock=99999999&sort=asc";
        if (!force && RequestCache.getInstance().contains(RequestCache.TYPE_TXS_INTERNAL, address)) {
            b.onResponse(null, new Response.Builder().code(200).message("").request(new Request.Builder()
                    .url(url)
                    .build()).protocol(Protocol.HTTP_1_0).body(ResponseBody.create(MediaType.parse("JSON"), RequestCache.getInstance().get(RequestCache.TYPE_TXS_INTERNAL, address))).build());
            return;
        }
        get(url, b);
    }


    /**
     * Retrieve all normal ether transactions from address (excluding contract calls etc, @see rehanced.com.simpleetherwallet.network.EtherscanAPI#getInternalTransactions() )
     *
     * @param address Ether address
     * @param b       Network callback to @see rehanced.com.simpleetherwallet.fragments.FragmentTransactions#update() or @see rehanced.com.simpleetherwallet.fragments.FragmentTransactionsAll#update()
     * @param force   Whether to force (true) a network call or use cache (false). Only true if user uses swiperefreshlayout
     * @throws IOException Network exceptions
     */
    public void getNormalTransactions(String address, Callback b, boolean force) throws IOException {
        String url = apiUrl + "txlist?address=" + address + "&startblock=0&endblock=99999999&sort=asc";
        if (!force && RequestCache.getInstance().contains(RequestCache.TYPE_TXS_NORMAL, address)) {
            b.onResponse(null, new Response.Builder().code(200).message("").request(new Request.Builder()
                    .url(url)
                    .build()).protocol(Protocol.HTTP_1_0).body(ResponseBody.create(MediaType.parse("JSON"), RequestCache.getInstance().get(RequestCache.TYPE_TXS_NORMAL, address))).build());
            return;
        }
        get(url, b);
    }


    public void getEtherPrice(Callback b) throws IOException {
        get(apiUrl + "currentPrice", b);
    }


    public void getGasPrice(Callback b) throws IOException {
        get(apiUrl + "gasPrice", b);
    }


    /**
     * Get token balances via ethplorer.io
     *
     * @param address Ether address
     * @param b       Network callback to @see rehanced.com.simpleetherwallet.fragments.FragmentDetailOverview#update()
     * @param force   Whether to force (true) a network call or use cache (false). Only true if user uses swiperefreshlayout
     * @throws IOException Network exceptions
     */
    public void getTokenBalances(String address, Callback b, boolean force) throws IOException {
        if (!force && RequestCache.getInstance().contains(RequestCache.TYPE_TOKEN, address)) {
            b.onResponse(null, new Response.Builder().code(200).message("").request(new Request.Builder()
                    .url("https://api.ethplorer.io/getAddressInfo/" + address + "?apiKey=freekey")
                    .build()).protocol(Protocol.HTTP_1_0).body(ResponseBody.create(MediaType.parse("JSON"), RequestCache.getInstance().get(RequestCache.TYPE_TOKEN, address))).build());
            return;
        }
        get("http://api.ethplorer.io/getAddressInfo/" + address + "?apiKey=freekey", b);
    }


    /**
     * Download and save token icon in permanent image cache (TokenIconCache)
     *
     * @param c         Application context, used to load TokenIconCache if reinstanced
     * @param tokenName Name of token
     * @param lastToken Boolean defining whether this is the last icon to download or not. If so callback is called to refresh recyclerview (notifyDataSetChanged)
     * @param callback  Callback to @see rehanced.com.simpleetherwallet.fragments.FragmentDetailOverview#onLastIconDownloaded()
     * @throws IOException Network exceptions
     */
    public void loadTokenIcon(final Context c, String tokenName, final boolean lastToken, final LastIconLoaded callback) throws IOException {
        if (tokenName.indexOf(" ") > 0)
            tokenName = tokenName.substring(0, tokenName.indexOf(" "));
        if (TokenIconCache.getInstance(c).contains(tokenName)) return;

        if(tokenName.equalsIgnoreCase("OMGToken"))
            tokenName = "omise";
        else if(tokenName.equalsIgnoreCase("0x"))
            tokenName = "0xtoken_28";

        final String tokenNamef = tokenName;
        get("http://etherscan.io//token/images/" + tokenNamef + ".PNG", new Callback() {
            @Override
            public void onFailure(Call call, IOException e) {
            }

            @Override
            public void onResponse(Call call, Response response) throws IOException {
                if (c == null) return;
                ResponseBody in = response.body();
                InputStream inputStream = in.byteStream();
                BufferedInputStream bufferedInputStream = new BufferedInputStream(inputStream);
                final Bitmap bitmap = BitmapFactory.decodeStream(bufferedInputStream);
                TokenIconCache.getInstance(c).put(c, tokenNamef, new BitmapDrawable(c.getResources(), bitmap).getBitmap());
                // if(lastToken) // TODO: resolve race condition
                callback.onLastIconDownloaded();
            }
        });
    }


    public void getGasLimitEstimate(String to, Callback b) throws IOException {
        String url = apiUrl + "estimateGas?to=" + to + "&value=0xff22&gasPrice=0x051da038cc&gas=0xffffff";
        get(url, b);
    }


    public void getBalance(String address, Callback b) throws IOException {
        String url = apiUrl + "account_balance?address=" + address;
        get(url, b);
    }


    public void getNonceForAddress(String address, Callback b) throws IOException {
        String url = apiUrl + "getTransactionCount?data=" + address + "&tag=latest";
        get(url, b);
    }


    public void getPriceConversionRates(String currencyConversion, Callback b) throws IOException {
        get("https://api.fixer.io/latest?base=USD&symbols=" + currencyConversion, b);
    }


    public void getBalances(ArrayList<StorableWallet> addresses, Callback b) throws IOException {
        String url = apiUrl + "account_balancemulti?address=";
        for (StorableWallet address : addresses)
            url += address.getPubKey() + ",";
        url = url.substring(0, url.length() - 1);
        get(url, b);
    }


    public void forwardTransaction(String raw, Callback b) throws IOException {
        String url = apiUrl + "sendRawTransaction?hex=" + raw;
        get(url, b);
    }


    public void get(String url, Callback b) throws IOException {
        Request request = new Request.Builder()
                .url(url)
                .build();
        OkHttpClient client = new OkHttpClient.Builder()
                .connectTimeout(10, TimeUnit.SECONDS)
                .writeTimeout(10, TimeUnit.SECONDS)
                .readTimeout(30, TimeUnit.SECONDS)
                .build();

        client.newCall(request).enqueue(b);
    }
}
