package me.zsr.hulu;

import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.TypedValue;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.List;

import me.zsr.hulu.view.ArticleUtil;
import me.zsr.hulu.R;
import me.zsr.rssbean.Article;
import me.zsr.rssbean.Subscription;
import me.zsr.rsscommon.AnimationHelper;
import me.zsr.rsscommon.DateUtil;
import me.zsr.rsscommon.IntentUtil;
import me.zsr.rsscommon.SPManager;
import me.zsr.rsscommon.StringUtil;
import me.zsr.rsscommon.ThreadManager;
import me.zsr.rsscommon.htmltextview.HtmlTextView;
import me.zsr.rsscommon.htmltextview.custom.LinkMovementMethodEx;
import me.zsr.viewmodel.ModelProxy;

import static me.zsr.hulu.Constants.*;

// TODO: 10/30/16 to be modularity
public class ArticleActivity extends AppCompatActivity implements View.OnClickListener, LinkMovementMethodEx.OnPicClickListener {
    private Toolbar mToolbar;
    private ScrollView mScrollView;
    private HtmlTextView mContentTextView;
    private TextView mTitleTextView;
    private TextView mSubscriptionNameTextView;
    private TextView mDateTextView;
    private TextView mTimeTextView;
    private Article mArticle;
    private long[] mIdArray;
    private int mCurrentIndex;

    private ImageButton mPreviousBtn;
    private ImageButton mNextBtn;

    private boolean mIsClickEnabled= true;
    private boolean mIsLoading;

    private ModelProxy mModel = new ModelProxy();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_article);

        initViews();
        mIdArray = getIntent().getExtras().getLongArray(Constants.KEY_BUNDLE_ARTICLE_ID);
        mCurrentIndex = getIntent().getExtras().getInt(Constants.KEY_BUNDLE_ARTICLE_INDEX);
        loadDataAsync(mIdArray[mCurrentIndex]);
    }

    private void initViews() {
        mToolbar = findViewById(R.id.toolbar);
        mToolbar.setNavigationIcon(getResources().getDrawable(R.drawable.round_arrow_back_black_24));
        mToolbar.setNavigationOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mToolbar.inflateMenu(R.menu.menu_article);
        mToolbar.setOnMenuItemClickListener(new Toolbar.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                switch (item.getItemId()) {
                    case R.id.action_link:
                        if (mArticle == null || StringUtil.isNullOrEmpty(mArticle.getLink())) {
                            return false;
                        }
                        IntentUtil.openUrl(ArticleActivity.this, mArticle.getLink());
                        break;
                    case R.id.action_fav:
                        if (mArticle != null) {
                            if (mArticle.getFavorite()) {
                                mArticle.setFavorite(false);
                                item.setIcon(R.drawable.round_star_border_black_24);
                                Toast.makeText(ArticleActivity.this, R.string.unfavorited, Toast.LENGTH_SHORT).show();
                            } else {
                                mArticle.setFavorite(true);
                                item.setIcon(R.drawable.round_star_black_24);
                                Toast.makeText(ArticleActivity.this, R.string.favorited, Toast.LENGTH_SHORT).show();
                            }
                            mModel.saveArticle(mArticle);
                        }
                        break;
                    case R.id.action_share:
                        shareToOthers(mArticle);
                        break;
                }
                return false;
            }
        });

        mScrollView = findViewById(R.id.scroll_container);

        mTitleTextView = findViewById(R.id.article_title);
        mDateTextView = findViewById(R.id.article_date);
        mTimeTextView = findViewById(R.id.article_time);
        mSubscriptionNameTextView = findViewById(R.id.subscription_name);

        mContentTextView = findViewById(R.id.article_content);
        switch (SPManager.getInt(KEY_FONT_SIZE, FONT_SIZE_MEDIUM)) {
            case FONT_SIZE_SMALL:
                mContentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.text_size_small));
                break;
            case FONT_SIZE_MEDIUM:
                mContentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.text_size_medium));
                break;
            case FONT_SIZE_BIG:
                mContentTextView.setTextSize(TypedValue.COMPLEX_UNIT_PX,
                        getResources().getDimension(R.dimen.text_size_big));
                break;
        }
        mContentTextView.setOnPicClickListener(this);

        mPreviousBtn = findViewById(R.id.previous_btn);
        mPreviousBtn.setOnClickListener(this);
        mNextBtn = findViewById(R.id.next_btn);
        mNextBtn.setOnClickListener(this);
    }

    private void loadDataAsync(final Long articleId) {
        mIsLoading = true;
        ThreadManager.postInBackground(new Runnable() {
            @Override
            public void run() {
                final List<Article> articleList = mModel.queryArticles(articleId);
                if (articleList.size() != 1) {
                    return;
                }
                mArticle = articleList.get(0);

                final List<Subscription> subscriptionList = mModel.querySubscriptions(mArticle.getSubscriptionId());
                if (subscriptionList.size() != 1) {
                    return;
                }

                if (!mArticle.getRead()) {
                    mModel.markAllRead(true, mArticle);
                }

                ThreadManager.post(new Runnable() {
                    @Override
                    public void run() {
                        setData(mArticle, subscriptionList.get(0).getTitle());
                        mIsLoading = false;
                    }
                });
            }
        });
    }

    private void setData(Article article, String subscriptionName) {
        mTitleTextView.setText(article.getTitle());
        mDateTextView.setText(DateUtil.formatDate(this, article.getPublished()));
        mTimeTextView.setText(DateUtil.formatTime(article.getPublished()));
        mSubscriptionNameTextView.setText(subscriptionName);
        ArticleUtil.setContent(this, article, mContentTextView, subscriptionName);
        mScrollView.scrollTo(0, 0);
        if (StringUtil.isNullOrEmpty(article.getLink())) {
            mToolbar.getMenu().findItem(R.id.action_link).setVisible(false);
        } else {
            mToolbar.getMenu().findItem(R.id.action_link).setVisible(true);
        }
        if (article.getFavorite()) {
            mToolbar.getMenu().findItem(R.id.action_fav).setIcon(R.drawable.round_star_black_24);
        } else {
            mToolbar.getMenu().findItem(R.id.action_fav).setIcon(R.drawable.round_star_border_black_24);
        }

        if (mCurrentIndex == 0) {
            mPreviousBtn.setAlpha(0.2f);
            mPreviousBtn.setClickable(false);
        } else {
            mPreviousBtn.setAlpha(1f);
            mPreviousBtn.setClickable(true);
        }
        if (mCurrentIndex == mIdArray.length - 1) {
            mNextBtn.setAlpha(0.2f);
            mNextBtn.setClickable(false);
        } else {
            mNextBtn.setAlpha(1f);
            mNextBtn.setClickable(true);
        }
    }

    public void shareToOthers(Article article) {
        Intent sendIntent = new Intent();
        sendIntent.setAction(Intent.ACTION_SEND);
        sendIntent.putExtra(Intent.EXTRA_TEXT, article.getLink());
        sendIntent.putExtra(Intent.EXTRA_TITLE, article.getTitle());
        sendIntent.putExtra("source", Constants.PACKAGE_NAME);
        sendIntent.setType("text/plain");
        startActivity(sendIntent);
    }

    @Override
    public void onClick(View v) {
        if (!mIsClickEnabled) {
            return;
        }
        recordClick();

        if (v == mPreviousBtn) {
            if (!mIsLoading) {
                loadDataAsync(mIdArray[--mCurrentIndex]);
            }
        } else if (v == mNextBtn) {
            if (!mIsLoading) {
                loadDataAsync(mIdArray[++mCurrentIndex]);
            }
        }
    }

    private void recordClick() {
        mIsClickEnabled = false;
        ThreadManager.postDelay(new Runnable() {
            @Override
            public void run() {
                mIsClickEnabled = true;
            }
        }, 500);
    }

    @Override
    public void onPicClick(String url) {
        Intent intent = new Intent(this, PicActivity.class);
        Bundle bundle = new Bundle();
        bundle.putString(Constants.KEY_BUNDLE_PIC_URL, url);
        intent.putExtras(bundle);
        startActivity(intent);

        AnimationHelper.setFadeTransition(this);
    }
}
