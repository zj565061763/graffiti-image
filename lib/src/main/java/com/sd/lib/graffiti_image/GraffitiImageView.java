package com.sd.lib.graffiti_image;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.LinearInterpolator;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片涂鸦
 */
public class GraffitiImageView extends View
{
    public GraffitiImageView(Context context)
    {
        super(context);
    }

    public GraffitiImageView(Context context, AttributeSet attrs)
    {
        super(context, attrs);
    }

    public GraffitiImageView(Context context, AttributeSet attrs, int defStyleAttr)
    {
        super(context, attrs, defStyleAttr);
    }

    private Mode mMode = Mode.draw;
    private final List<Group> mGroups = new ArrayList<>();
    private int mGroupSize;

    private Integer mBoundsWidth;
    private Integer mBoundsHeight;

    private GraffitiAnimator mGraffitiAnimator;

    private GroupSizeChangeCallback mGroupSizeChangeCallback;
    private AnimatorCallback mAnimatorCallback;

    /**
     * 设置组数量变化回调
     *
     * @param callback
     */
    public void setGroupSizeChangeCallback(GroupSizeChangeCallback callback)
    {
        mGroupSizeChangeCallback = callback;
    }

    /**
     * 设置动画回调
     *
     * @param callback
     */
    public void setAnimatorCallback(AnimatorCallback callback)
    {
        mAnimatorCallback = callback;
    }

    /**
     * 设置Item的边界大小。
     * <p>
     * 1.如果为null，则边界大小为当前View的边界大小
     * 2.如果不为null，则Item的坐标会根据指定的边界大小计算出百分比之后，映射到当前View的边界内
     *
     * @param boundsWidth
     * @param boundsHeight
     */
    public void setItemBounds(Integer boundsWidth, Integer boundsHeight)
    {
        mBoundsWidth = boundsWidth;
        mBoundsHeight = boundsHeight;
    }

    /**
     * 添加组
     *
     * @param group
     */
    public void addGroup(Group group)
    {
        if (group == null)
            return;
        mGroups.add(group);
        notifyGroupSizeIfNeed();
    }

    /**
     * 设置组
     *
     * @param list
     */
    public void setGroups(List<Group> list)
    {
        if (list == null || list.isEmpty())
            return;

        mGroups.clear();
        mGroups.addAll(list);
        notifyGroupSizeIfNeed();
    }

    /**
     * 移除最后一组
     */
    public void removeLastGroup()
    {
        if (mGroups.isEmpty())
            return;

        mGroups.remove(mGroups.size() - 1);
        notifyGroupSizeIfNeed();
    }

    /**
     * 返回一共有几组
     *
     * @return
     */
    public int groupSize()
    {
        return mGroups.size();
    }

    /**
     * Item的总数量
     *
     * @return
     */
    public int itemSize()
    {
        if (mGroups.isEmpty())
            return 0;

        int size = 0;
        for (Group group : mGroups)
        {
            size += group.itemSize();
        }
        return size;
    }

    /**
     * 显示涂鸦
     */
    public void showGraffiti()
    {
        setMode(Mode.draw);
        invalidate();
    }

    /**
     * 涂鸦动画
     *
     * @return
     */
    public GraffitiAnimator getGraffitiAnimator()
    {
        if (mGraffitiAnimator == null)
            mGraffitiAnimator = new GraffitiAnimator();
        return mGraffitiAnimator;
    }

    /**
     * 设置模式
     *
     * @param mode
     */
    private void setMode(Mode mode)
    {
        if (mode == null)
            return;

        if (mMode != mode)
        {
            mMode = mode;

            if (mode == Mode.draw)
                stopGraffitiAnimator();
        }
    }

    private int getBoundsWidth()
    {
        int result = getWidth();
        if (mBoundsWidth != null && mBoundsWidth > 0)
            result = mBoundsWidth;

        return result;
    }

    private int getBoundsHeight()
    {
        int result = getHeight();
        if (mBoundsHeight != null && mBoundsHeight > 0)
            result = mBoundsHeight;

        return result;
    }

    @Override
    protected void onDraw(Canvas canvas)
    {
        super.onDraw(canvas);
        final int width = getWidth();
        final int height = getHeight();
        if (width > 0 && height > 0)
        {
            if (mMode == Mode.draw)
            {
                drawDrawMode(canvas, width, height);
            } else if (mMode == Mode.animator)
            {
                drawAnimatorMode(canvas, width, height);
            }
        }
    }

    private void drawDrawMode(Canvas canvas, int width, int height)
    {
        for (Group group : mGroups)
        {
            for (Item item : group.getItems())
            {
                drawItem(item, canvas, width, height);
            }
        }
    }

    private void drawAnimatorMode(Canvas canvas, int width, int height)
    {
        final GraffitiAnimator graffitiAnimator = mGraffitiAnimator;
        if (graffitiAnimator != null)
            graffitiAnimator.draw(canvas, width, height);
    }

    private void drawItem(Item item, Canvas canvas, int width, int height)
    {
        final Bitmap bitmap = item.getBitmap();
        final float transformLeft = item.x / getBoundsWidth() * width;
        final float transformTop = item.y / getBoundsHeight() * height;

        final float left = transformLeft - (bitmap.getWidth() / 2);
        final float top = transformTop - (bitmap.getHeight() / 2);

        canvas.drawBitmap(bitmap, left, top, null);
    }

    private void notifyGroupSizeIfNeed()
    {
        final int size = mGroups.size();
        if (mGroupSize != size)
        {
            mGroupSize = size;
            if (mGroupSizeChangeCallback != null)
                mGroupSizeChangeCallback.onGroupSizeChanged(size);
        }
    }

    /**
     * 停止涂鸦动画
     */
    private void stopGraffitiAnimator()
    {
        if (mGraffitiAnimator != null)
        {
            mGraffitiAnimator.stop();
            mGraffitiAnimator = null;
        }
    }

    @Override
    protected void onDetachedFromWindow()
    {
        super.onDetachedFromWindow();
        stopGraffitiAnimator();
    }

    public class GraffitiAnimator
    {
        private Animator mAnimator;
        private AnimatorMode mAnimatorMode;

        private Group mCurrentGroup;
        private int mCurrentGroupItemIndex;

        private Long mDuration;
        private List<Item> mListDurationItem;
        private int mDurationIndex;

        /**
         * 设置总的动画时长
         *
         * @param duration
         */
        public void setDuration(Long duration)
        {
            mDuration = duration;
        }

        /**
         * 开始动画
         */
        public void start()
        {
            if (mGroups.isEmpty())
                return;

            stop();

            if (mDuration == null)
            {
                startAnimatorByGroup();
            } else
            {
                startAnimatorByDuration(mDuration);
            }
        }

        private void startAnimator(Animator animator)
        {
            mAnimator = animator;
            mAnimator.addListener(new AnimatorListenerAdapter()
            {
                @Override
                public void onAnimationStart(Animator animation)
                {
                    super.onAnimationStart(animation);
                    if (mAnimatorCallback != null)
                        mAnimatorCallback.onAnimationStart();
                }

                @Override
                public void onAnimationEnd(Animator animation)
                {
                    super.onAnimationEnd(animation);
                    if (mAnimatorCallback != null)
                        mAnimatorCallback.onAnimationEnd();
                }
            });

            setMode(Mode.animator);
            mAnimator.start();
        }

        /**
         * 开始动画（组动画模式）
         */
        private void startAnimatorByGroup()
        {
            final List<Animator> listGroupAnimator = new ArrayList<>();
            for (final Group group : mGroups)
            {
                final int itemSize = group.itemSize();
                if (itemSize <= 0)
                    continue;

                final ValueAnimator animator = ValueAnimator.ofInt(0, itemSize - 1);
                animator.setDuration(group.getGroupDuration());
                animator.setInterpolator(new LinearInterpolator());
                animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
                {
                    @Override
                    public void onAnimationUpdate(ValueAnimator animation)
                    {
                        mCurrentGroup = group;
                        mCurrentGroupItemIndex = (int) animation.getAnimatedValue();
                        invalidate();
                    }
                });

                listGroupAnimator.add(animator);
            }

            if (listGroupAnimator.isEmpty())
                return;

            final AnimatorSet animatorSet = new AnimatorSet();
            animatorSet.playSequentially(listGroupAnimator);

            mAnimatorMode = AnimatorMode.group;
            startAnimator(animatorSet);
        }

        /**
         * 开始动画（总时长模式）
         */
        private void startAnimatorByDuration(long duration)
        {
            final List<Item> listItem = new ArrayList<>();
            for (final Group group : mGroups)
            {
                listItem.addAll(group.getItems());
            }

            if (listItem.isEmpty())
                return;

            final ValueAnimator animator = ValueAnimator.ofInt(0, listItem.size() - 1);
            animator.setInterpolator(new LinearInterpolator());
            animator.addUpdateListener(new ValueAnimator.AnimatorUpdateListener()
            {
                @Override
                public void onAnimationUpdate(ValueAnimator animation)
                {
                    mDurationIndex = (int) animation.getAnimatedValue();
                    invalidate();
                }
            });
            animator.setDuration(duration);

            mListDurationItem = listItem;
            mAnimatorMode = AnimatorMode.duration;
            startAnimator(animator);
        }

        /**
         * 绘制Item
         *
         * @param canvas
         * @param width
         * @param height
         */
        private void draw(Canvas canvas, int width, int height)
        {
            if (mAnimatorMode == AnimatorMode.group)
            {
                drawByGroup(canvas, width, height);
            } else if (mAnimatorMode == AnimatorMode.duration)
            {
                drawByDuration(canvas, width, height);
            }
        }

        /**
         * 绘制Item（组动画模式）
         *
         * @param canvas
         * @param width
         * @param height
         */
        private void drawByGroup(Canvas canvas, int width, int height)
        {
            if (mCurrentGroup == null)
                return;

            for (Group group : mGroups)
            {
                if (group == mCurrentGroup)
                {
                    int index = 0;
                    for (Item item : group.getItems())
                    {
                        drawItem(item, canvas, width, height);

                        if (index == mCurrentGroupItemIndex)
                            break;
                        index++;
                    }
                    break;
                } else
                {
                    for (Item item : group.getItems())
                    {
                        drawItem(item, canvas, width, height);
                    }
                }
            }
        }

        /**
         * 绘制Item（总时长模式）
         *
         * @param canvas
         * @param width
         * @param height
         */
        private void drawByDuration(Canvas canvas, int width, int height)
        {
            final List<Item> listItem = mListDurationItem;
            if (listItem == null || listItem.isEmpty())
                return;

            int index = 0;
            for (Item item : listItem)
            {
                drawItem(item, canvas, width, height);

                if (index == mDurationIndex)
                    break;
                index++;
            }
        }

        /**
         * 停止动画
         */
        public void stop()
        {
            if (mAnimator != null)
            {
                mAnimator.cancel();
                mAnimator = null;
            }

            mCurrentGroup = null;
            mCurrentGroupItemIndex = 0;

            mDurationIndex = 0;
            mListDurationItem = null;
        }
    }

    private enum Mode
    {
        draw,
        animator
    }

    private enum AnimatorMode
    {
        group,
        duration
    }

    public static class Group
    {
        private final Bitmap bitmap;
        private List<Item> items;

        private long itemDuration = 100;
        private int itemSpacing;

        public Group(Bitmap bitmap)
        {
            this.bitmap = bitmap;
        }

        private List<Item> getItems()
        {
            if (items == null)
                items = new ArrayList<>();
            return items;
        }

        /**
         * 设置每个Item的动画时长
         *
         * @param duration
         */
        public void setItemDuration(long duration)
        {
            if (duration < 0)
                duration = 100;
            this.itemDuration = duration;
        }

        /**
         * 设置Item间距
         *
         * @param spacing
         */
        public void setItemSpacing(int spacing)
        {
            this.itemSpacing = spacing;
        }

        /**
         * 添加Item
         *
         * @param item
         */
        public void addItem(Item item)
        {
            if (item == null)
                return;

            getItems().add(item);
            item.group = this;
        }

        /**
         * Item数量
         *
         * @return
         */
        public int itemSize()
        {
            return items == null ? 0 : items.size();
        }

        /**
         * 返回最后一个Item
         *
         * @return
         */
        public Item lastItem()
        {
            final int itemSize = itemSize();
            if (itemSize <= 0)
                return null;

            return items.get(itemSize - 1);
        }

        /**
         * 当前组的总动画时长
         *
         * @return
         */
        public long getGroupDuration()
        {
            final int itemSize = itemSize();
            if (itemSize <= 0)
                return 0;

            return itemSize * itemDuration;
        }
    }

    public static class Item
    {
        private final float x;
        private final float y;

        private Bitmap bitmap;
        Group group;

        public Item(float x, float y)
        {
            this.x = x;
            this.y = y;
        }

        public float getX()
        {
            return x;
        }

        public float getY()
        {
            return y;
        }

        Bitmap getBitmap()
        {
            Bitmap result = group == null ? null : group.bitmap;
            if (bitmap != null)
                result = bitmap;
            return result;
        }

        int getHorizontalSpacing()
        {
            final Bitmap bitmap = getBitmap();
            int result = bitmap == null ? 0 : bitmap.getWidth();

            if (group != null)
                result += group.itemSpacing;

            return result;
        }

        int getVerticalSpacing()
        {
            final Bitmap bitmap = getBitmap();
            int result = bitmap == null ? 0 : bitmap.getHeight();

            if (group != null)
                result += group.itemSpacing;

            return result;
        }
    }

    public interface AnimatorCallback
    {
        void onAnimationStart();

        void onAnimationEnd();
    }

    public interface GroupSizeChangeCallback
    {
        void onGroupSizeChanged(int size);
    }
}