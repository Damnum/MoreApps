package com.agiliq.android.moreappslib.getappsdetails;

import com.gc.android.market.api.MarketSession;
import com.gc.android.market.api.MarketSession.Callback;
import com.gc.android.market.api.model.Market.App;
import com.gc.android.market.api.model.Market.AppsRequest;
import com.gc.android.market.api.model.Market.AppsResponse;
import com.gc.android.market.api.model.Market.ResponseContext;
import com.gc.android.market.api.model.Market.GetImageRequest;
import com.gc.android.market.api.model.Market.GetImageRequest.AppImageUsage;
import com.gc.android.market.api.model.Market.GetImageResponse;

import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

public class GetAppsDetails implements Callback<AppsResponse> {

    private int typeOfListItems;
    private static final int LIST_ITEMS_TITLE = 0;
    private static final int LIST_ITEMS_PACKAGE_NAME = 1;
    private static final int LIST_ITEMS_ID = 2;

    MarketSession session;
    int entriesCount = -1;
    int countOfApps = 0;
    ArrayList<String> appTitles;
    ArrayList<String> appPackages;
    ArrayList<String> appIds;

    public GetAppsDetails() {

        appTitles = new ArrayList<String>();
        appPackages = new ArrayList<String>();
        appIds = new ArrayList<String>();

        session = new MarketSession();

        String email = "xXxXxXxXxXx@email.com";
        String password = "yYyYyYyYyYy";
        session.login(email, password);

        session.getContext().setAndroidId("8");

        String query = "pub:Agiliq";
        retrieveTitles_Packages_AppIds(query);
        buildCSV();
        storeImages();
    }

    private void retrieveTitles_Packages_AppIds(String query) {
        do {
            AppsRequest appsRequest = createAppsRequestWithQuery(query, countOfApps);

            session.append(appsRequest, this);
            session.flush();
        } while (countOfApps < entriesCount);
    }

    private AppsRequest createAppsRequestWithQuery(String query, int index) {
        return AppsRequest.newBuilder()
                .setQuery(query)
                .setStartIndex(index).setEntriesCount(10)
                .setWithExtendedInfo(true)
                .build();
    }

    @Override
    public void onResult(ResponseContext context, AppsResponse response) {
        if (entriesCount < 0)
            entriesCount = response.getEntriesCount();

        appTitles.addAll(getTitles(response));
        appPackages.addAll(getPackageNames(response));
        appIds.addAll(getIds(response));

        countOfApps = appTitles.size();
    }

    private ArrayList<String> getTitles(AppsResponse response) {
        typeOfListItems = LIST_ITEMS_TITLE;
        return getItems(response);
    }

    private ArrayList<String> getPackageNames(AppsResponse response) {
        typeOfListItems = LIST_ITEMS_PACKAGE_NAME;
        return getItems(response);
    }

    private ArrayList<String> getIds(AppsResponse response) {
        typeOfListItems = LIST_ITEMS_ID;
        return getItems(response);
    }

    private ArrayList<String> getItems(AppsResponse response) {
        List<App> appList = response.getAppList();
        ArrayList<String> items = new ArrayList<String>();
        switch (typeOfListItems) {
            case LIST_ITEMS_TITLE: {
                for (App app : appList) {
                    items.add(app.getTitle());
                }
                break;
            }
            case LIST_ITEMS_PACKAGE_NAME: {
                for (App app : appList) {
                    items.add(app.getPackageName());
                }
                break;
            }
            case LIST_ITEMS_ID: {
                for (App app : appList) {
                    items.add(app.getId());
                }
            }
        }

        return items;
    }

    private void buildCSV() {
        FileOutputStream fos;    // Facts are saved in a CSV file.
        try {
            fos = new FileOutputStream("Agiliq_apps.csv");
            for (int i = 0; i < entriesCount; i++) {
                String line = appTitles.get(i) + ",," + appPackages.get(i) + "\n";
                byte[] csvLine = line.getBytes();
                fos.write(csvLine);
            }
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void storeImages() {
        for (countOfApps = 0; countOfApps < appIds.size(); countOfApps++) {
            String appId = appIds.get(countOfApps);

            GetImageRequest imgReq = GetImageRequest.newBuilder().setAppId(appId)
                    .setImageUsage(AppImageUsage.ICON)
                    .setImageId("1")
                    .build();

            session.append(imgReq, new Callback<GetImageResponse>() {

                @Override
                public void onResult(ResponseContext context, GetImageResponse response) {
                    try {
                        FileOutputStream fos = new FileOutputStream(countOfApps + ".png");
                        fos.write(response.getImageData().toByteArray());
                        fos.close();
                    } catch (IOException ex) {
                        ex.printStackTrace();
                    }
                }
            });
            session.flush();
        }
    }
}