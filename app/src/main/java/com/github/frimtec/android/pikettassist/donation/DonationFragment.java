package com.github.frimtec.android.pikettassist.donation;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Html;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.res.ResourcesCompat;
import androidx.fragment.app.DialogFragment;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.QueryProductDetailsParams.Product;
import com.github.frimtec.android.pikettassist.R;
import com.github.frimtec.android.pikettassist.donation.billing.BillingProvider;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;

public class DonationFragment extends DialogFragment {

  private static final String TAG = "DonationFragment";

  private RecyclerView recyclerView;
  private ArticleAdapter adapter;
  private TextView errorTextView;
  private BillingProvider billingProvider;
  private boolean noBillingService = false;

  private static final String PAYPAL_ME_BASE_LINK = "https://paypal.me/frimtec";
  private static final String PAYPAL_ME_LINK = PAYPAL_ME_BASE_LINK + "?country.x=CH&locale.x=de_DE";
  private static final String ALTERNATIVE_DONATION_TEXT_TEMPLATE =
      "<h3>" +
      "%s" +
      "</h3>" +
      "</p>" +
      "<p>" +
      "<br>" +
      "<a href=\"" + PAYPAL_ME_LINK + "\">" +
      "<img src=\"paypalme.png\"/>" +
      "</<a>" +
      "<br>" +
      "<br>" +
      "<h1>" +
      "<a href=\"" + PAYPAL_ME_LINK + "\">" +
      PAYPAL_ME_BASE_LINK +
      "</a>" +
      "</h1>" +
      "</p>" +
      "<p>" +
      "<br>" +
      "<h3>" +
      "%s" +
      "</h3>" +
      "</p>";

  @Override
  public void onCreate(@Nullable Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    setStyle(DialogFragment.STYLE_NORMAL, R.style.AppTheme);
  }

  @Nullable
  @Override
  public View onCreateView(
      LayoutInflater inflater,
      @Nullable ViewGroup container,
      @Nullable Bundle savedInstanceState
  ) {
    View root = inflater.inflate(R.layout.donation_fragment, container, false);
    this.errorTextView = root.findViewById(R.id.error_textview);
    this.recyclerView = root.findViewById(R.id.list);

    if (this.noBillingService) {
      TextView alternativeDonationText = root.findViewById(R.id.alternative_donation_text);
      alternativeDonationText.setVisibility(View.VISIBLE);
      alternativeDonationText.setText(Html.fromHtml(
          String.format(
              ALTERNATIVE_DONATION_TEXT_TEMPLATE,
              getString(R.string.alternative_donation_text),
              getString(R.string.alternative_donation_thanks)
          ),
          Html.FROM_HTML_MODE_COMPACT,
          source -> {
            Drawable image = ResourcesCompat.getDrawable(getResources(), R.drawable.paypalme, null);
            if (image != null) {
              image.setBounds(0, 0, 550, 215);
            }
            return image;
          },
          null
      ));
      alternativeDonationText.setMovementMethod(LinkMovementMethod.getInstance());
      this.recyclerView.setVisibility(View.GONE);
      this.errorTextView.setVisibility(View.GONE);
    } else if (this.billingProvider != null) {
      handleManagerAndUiReady();
    }

    // Setup a toolbar for this fragment
    Toolbar toolbar = root.findViewById(R.id.toolbar);
    toolbar.setNavigationIcon(R.drawable.ic_arrow_up);
    toolbar.setNavigationOnClickListener(v -> dismiss());
    toolbar.setTitle(R.string.menu_donate);
    return root;
  }

  @SuppressLint("NotifyDataSetChanged")
  public void refreshUI() {
    if (this.adapter != null) {
      this.adapter.notifyDataSetChanged();
    }
  }

  /**
   * Notifies the fragment that billing manager is ready and provides a BillingProviders
   * instance to access it
   */
  public void onManagerReady(@Nullable BillingProvider billingProvider) {
    if (billingProvider != null) {
      this.billingProvider = billingProvider;
      this.noBillingService = false;
      if (this.recyclerView != null) {
        handleManagerAndUiReady();
      }
    } else {
      this.noBillingService = true;
    }
  }

  private void handleManagerAndUiReady() {
    queryProductDetails();
  }

  private void displayAnErrorIfNeeded() {
    if (getActivity() == null || getActivity().isFinishing()) {
      return;
    }
    this.errorTextView.setVisibility(View.VISIBLE);
    int billingResponseCode = billingProvider.getBillingManager().getBillingClientResponseCode();

    if (billingResponseCode == BillingClient.BillingResponseCode.BILLING_UNAVAILABLE) {
      this.errorTextView.setText(getText(R.string.error_billing_unavailable));
    } else {
      this.errorTextView.setText(getText(R.string.error_billing_default));
    }
  }

  private void queryProductDetails() {
    if (getActivity() != null && !getActivity().isFinishing()) {
      final List<ProductRowData> dataList = new ArrayList<>();
      this.adapter = new ArticleAdapter();
      UiManager uiManager = createUiManager(this.adapter, this.billingProvider);
      this.adapter.setUiManager(uiManager);
      // Once we added all the subscription items, fill the in-app items rows below
      addProductRows(dataList, uiManager.getDelegatesFactory().getProductList());
    }
  }

  private void addProductRows(List<ProductRowData> inList, List<Product> products) {
    this.billingProvider.getBillingManager().querySkuDetailsAsync(products,
        (billingResult, productDetails) -> {
          FragmentActivity activity = getActivity();
          if (activity != null) {
            activity.runOnUiThread(() -> {
              if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Unsuccessful query. Error code: " + billingResult.getResponseCode());
              } else if (productDetails.size() > 0) {
                // If we successfully got SKUs, add a header in front of the row
                inList.add(new ProductRowData(getString(R.string.header_inapp)));
                productDetails.stream()
                    .sorted(
                        Comparator.comparing((ProductDetails details) -> Objects.requireNonNull(details.getOneTimePurchaseOfferDetails()).getPriceAmountMicros())
                            .reversed()
                    ).forEach(productDetail -> inList.add(new ProductRowData(productDetail)));
                if (inList.size() == 1) {
                  displayAnErrorIfNeeded();
                } else {
                  if (this.recyclerView.getAdapter() == null) {
                    this.recyclerView.setAdapter(this.adapter);
                    Context context = getContext();
                    if (context != null) {
                      Resources res = getContext().getResources();
                      this.recyclerView.addItemDecoration(new CardsWithHeadersDecoration(this.adapter, (int) res.getDimension(R.dimen.header_gap), (int) res.getDimension(R.dimen.row_gap)));
                    }
                    this.recyclerView.setLayoutManager(new LinearLayoutManager(getContext()));
                  }
                  this.adapter.updateData(inList);
                }
              } else {
                displayAnErrorIfNeeded();
              }
            });
          }
        });
  }

  private UiManager createUiManager(ArticleAdapter adapter, BillingProvider provider) {
    return new UiManager(adapter, provider);
  }
}