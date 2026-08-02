package ru.noxen.implement.features.draggables;

import dev.redstones.mediaplayerinfo.IMediaSession;
import dev.redstones.mediaplayerinfo.MediaInfo;
import dev.redstones.mediaplayerinfo.MediaPlayerInfo;
import net.minecraft.client.gui.DrawContext;
import net.minecraft.client.texture.NativeImage;
import net.minecraft.client.texture.NativeImageBackedTexture;
import net.minecraft.client.texture.TextureManager;
import net.minecraft.client.util.math.MatrixStack;
import net.minecraft.util.Identifier;
import ru.noxen.api.feature.draggable.AbstractDraggable;
import ru.noxen.api.system.animation.Direction;
import ru.noxen.api.system.animation.implement.DecelerateAnimation;
import ru.noxen.api.system.font.FontRenderer;
import ru.noxen.api.system.font.Fonts;
import ru.noxen.api.system.shape.ShapeProperties;
import ru.noxen.common.QuickImports;
import ru.noxen.common.util.color.ColorUtil;
import ru.noxen.common.util.other.Instance;
import ru.noxen.common.util.render.ScissorManager;
import ru.noxen.core.Main;
import ru.noxen.common.util.world.ServerUtil;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Comparator;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class DynamicIsland extends AbstractDraggable implements QuickImports {

    public static DynamicIsland getInstance() {
        return Instance.getDraggable(DynamicIsland.class);
    }

    private final DecelerateAnimation internetAnimation = new DecelerateAnimation();
    private final DecelerateAnimation mediaAnimation = new DecelerateAnimation();
    private final DecelerateAnimation pvpAnimation = new DecelerateAnimation();
    private final DecelerateAnimation barAnimation = new DecelerateAnimation();
    private final DecelerateAnimation moduleAnimation = new DecelerateAnimation();

    private final float[] targetBarHeights = new float[]{10f, 8f, 6f};
    private final float[] currentBarHeights = new float[]{10f, 8f, 6f};
    private long lastUpdateTime = 0;

    private float currentWidth = 60f;
    private float currentHeight = 15f;

    private String currentModuleNotification = "";
    private String currentModuleNotificationClean = "";
    private long moduleNotificationTime = 0;
    private static final long MODULE_NOTIFICATION_DURATION = 2000;

    private static final Pattern PVP_TIMER_PATTERN = Pattern.compile("(\\d+)");

    private String trackName = null;
    private String artistsText = null;
    private IMediaSession activeSession = null;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private final AtomicBoolean polling = new AtomicBoolean(false);
    private volatile long lastPollMs = 0L;

    private final Identifier coverTextureLocation = Identifier.of("noxen", "music_cover_di");
    private NativeImageBackedTexture coverTexture = null;
    private int coverHash = 0;

    public DynamicIsland() {
        super("Dynamic Island", 0, 4, 100, 18, false);
        internetAnimation.setMs(300);
        internetAnimation.setValue(1);
        mediaAnimation.setMs(300);
        mediaAnimation.setValue(1);
        pvpAnimation.setMs(300);
        pvpAnimation.setValue(1);
        barAnimation.setMs(300);
        barAnimation.setValue(1);
        moduleAnimation.setMs(300);
        moduleAnimation.setValue(1);
    }

    @Override
    public boolean visible() {
        return ru.noxen.implement.features.modules.render.Hud.getInstance().isState()
                && ru.noxen.implement.features.modules.render.Hud.getInstance().interfaceSettings.isSelected("Dynamic Island");
    }

    @Override
    public void tick() {
        if (mc.player == null || mc.world == null) return;

        long now = System.currentTimeMillis();
        if (now - lastPollMs < 200L) {
            updateAnimationsAndWidth();
            return;
        }
        lastPollMs = now;

        if (!polling.compareAndSet(false, true)) {
            updateAnimationsAndWidth();
            return;
        }

        executor.execute(() -> {
            try {
                IMediaSession session = MediaPlayerInfo.Instance.getMediaSessions().stream()
                        .max(Comparator.comparing(s -> s.getMedia().getPlaying()))
                        .orElse(null);

                if (session != null) {
                    MediaInfo info = session.getMedia();
                    if (info != null && !info.getTitle().isEmpty()) {
                        String newTrackName = info.getTitle();
                        String newArtistsText = (info.getArtist() != null && !info.getArtist().isEmpty()) ? info.getArtist() : null;

                        byte[] newCover = info.getArtworkPng();
                        int newCoverHash = 0;
                        NativeImage decodedImage = null;

                        if (newCover != null && newCover.length > 0) {
                            try {
                                newCoverHash = Arrays.hashCode(newCover);
                                decodedImage = NativeImage.read(new ByteArrayInputStream(newCover));
                            } catch (Exception ignored) {
                                decodedImage = null;
                                newCoverHash = 0;
                            }
                        }

                        NativeImage finalDecodedImage = decodedImage;
                        int finalCoverHash = newCoverHash;
                        mc.execute(() -> {
                            activeSession = session;
                            trackName = newTrackName;
                            artistsText = newArtistsText;

                            if (newCover == null || newCover.length == 0) {
                                clearCoverTexture();
                                coverHash = 0;
                            } else if (finalDecodedImage != null) {
                                if (finalCoverHash != coverHash) {
                                    updateCoverTexture(finalDecodedImage);
                                    coverHash = finalCoverHash;
                                } else {
                                    finalDecodedImage.close();
                                }
                            } else {
                                clearCoverTexture();
                                coverHash = 0;
                            }
                        });
                    } else {
                        mc.execute(this::clearData);
                    }
                } else {
                    mc.execute(this::clearData);
                }
            } catch (Throwable t) {
                mc.execute(this::clearData);
            } finally {
                polling.set(false);
            }
        });

        updateAnimationsAndWidth();
    }

    private void updateAnimationsAndWidth() {
        int ping = 0;
        if (mc.player != null && mc.getNetworkHandler() != null && mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()) != null) {
            ping = mc.getNetworkHandler().getPlayerListEntry(mc.player.getUuid()).getLatency();
        }

        boolean isPvp = ServerUtil.isPvp();
        boolean mediaNull = (trackName == null || trackName.isEmpty());
        boolean hasActiveMusic = !mediaNull && !isPvp;
        boolean showModuleNotification = !currentModuleNotification.isEmpty()
                && System.currentTimeMillis() - moduleNotificationTime < MODULE_NOTIFICATION_DURATION;

        internetAnimation.setDirection(ping < 1000 ? Direction.FORWARDS : Direction.BACKWARDS);
        mediaAnimation.setDirection(isPvp || mediaNull ? Direction.BACKWARDS : Direction.FORWARDS);
        pvpAnimation.setDirection(isPvp ? Direction.FORWARDS : Direction.BACKWARDS);
        barAnimation.setDirection(hasActiveMusic ? Direction.FORWARDS : Direction.BACKWARDS);
        moduleAnimation.setDirection(showModuleNotification ? Direction.FORWARDS : Direction.BACKWARDS);

        if (hasActiveMusic && System.currentTimeMillis() - lastUpdateTime > 100) {
            updateBarHeights();
            lastUpdateTime = System.currentTimeMillis();
        }

        for (int i = 0; i < 3; i++) {
            currentBarHeights[i] = lerp(currentBarHeights[i], targetBarHeights[i], 0.3f);
        }

        // Figure out how wide the pill needs to be for whatever is currently shown,
        // then smoothly glide towards it instead of snapping instantly.
        float padding = 2f;
        FontRenderer font = Fonts.getSize(12, Fonts.Type.BOLD);
        float targetWidth;
        if (showModuleNotification) {
            targetWidth = 15 + font.getStringWidth(currentModuleNotificationClean) + padding * 2;
        } else if (isPvp) {
            targetWidth = 15 + font.getStringWidth("PVP") + padding * 3;
        } else if (!mediaNull) {
            String track = trackName != null ? trackName : "";
            String artist = artistsText != null ? artistsText : "";
            String fullTrack = track + (artist.isEmpty() ? "" : " - " + artist);
            targetWidth = 15 + font.getStringWidth(fullTrack) + padding * 2;
        } else {
            targetWidth = 15 + font.getStringWidth("Noxen") + padding * 2;
        }
        float targetHeight = 15f;

        currentWidth = lerp(currentWidth, targetWidth, 0.2f);
        currentHeight = lerp(currentHeight, targetHeight, 0.2f);
    }

    public void showModuleNotification(String moduleName, boolean enabled) {
        currentModuleNotification = moduleName + " " + (enabled ? "§aEnabled" : "§cDisabled");
        currentModuleNotificationClean = moduleName + " " + (enabled ? "Enabled" : "Disabled");
        moduleNotificationTime = System.currentTimeMillis();
        moduleAnimation.reset();
    }

    private float lerp(float a, float b, float t) {
        return a + (b - a) * t;
    }

    private void updateBarHeights() {
        for (int i = 0; i < 3; i++) {
            targetBarHeights[i] = 4 + (float) Math.random() * 8;
        }
    }

    private void clearData() {
        trackName = null;
        artistsText = null;
        activeSession = null;
        clearCoverTexture();
    }

    private void clearCoverTexture() {
        try {
            TextureManager tm = mc.getTextureManager();
            if (tm != null) tm.destroyTexture(coverTextureLocation);
            if (coverTexture != null) {
                coverTexture.close();
                coverTexture = null;
            }
        } catch (Exception ignored) {
            coverTexture = null;
        }
        coverHash = 0;
    }

    private void updateCoverTexture(NativeImage nativeImage) {
        try {
            TextureManager tm = mc.getTextureManager();
            if (tm != null) tm.destroyTexture(coverTextureLocation);
            if (coverTexture != null) {
                coverTexture.close();
                coverTexture = null;
            }
            coverTexture = new NativeImageBackedTexture(nativeImage);
            if (tm != null) tm.registerTexture(coverTextureLocation, coverTexture);
        } catch (Exception e) {
            clearCoverTexture();
            try {
                nativeImage.close();
            } catch (Exception ignored) {
            }
        }
    }

    private String getPvpTimer() {
        if (mc.inGameHud == null || mc.inGameHud.getBossBarHud() == null) return "0";
        for (var bar : mc.inGameHud.getBossBarHud().bossBars.values()) {
            String text = bar.getName().getString();
            if (text.toLowerCase().contains("pvp") || text.toLowerCase().contains("пвп")) {
                Matcher matcher = PVP_TIMER_PATTERN.matcher(text);
                if (matcher.find()) return matcher.group(1);
            }
        }
        return "0";
    }

    @Override
    public void drawDraggable(DrawContext context) {
        if (mc.player == null || mc.world == null) return;

        MatrixStack matrix = context.getMatrices();
        ScissorManager scissor = Main.getInstance().getScissorManager();

        String name = "Noxen";
        String track = trackName != null ? trackName : "";
        String artist = artistsText != null ? artistsText : "";
        String fullTrack = track + (artist.isEmpty() ? "" : " - " + artist);
        String pvp = "PVP";
        String pvpTimer = getPvpTimer();
        boolean isPvp = ServerUtil.isPvp();
        boolean mediaNull = (trackName == null || trackName.isEmpty());
        boolean showModuleNotification = !currentModuleNotification.isEmpty()
                && System.currentTimeMillis() - moduleNotificationTime < MODULE_NOTIFICATION_DURATION;

        float padding = 2f;
        float round = 6f;

        FontRenderer font = Fonts.getSize(12, Fonts.Type.BOLD);

        float baseHeight = 15f;
        float width = currentWidth;
        float height = currentHeight;
        float x = mc.getWindow().getScaledWidth() / 2f - width / 2f;

        float bossBarOffset = 0f;
        if (mc.inGameHud != null && mc.inGameHud.getBossBarHud() != null) {
            int bossBarCount = mc.inGameHud.getBossBarHud().bossBars.size();
            if (bossBarCount > 0) bossBarOffset = 19;
        }

        float y = 4f + bossBarOffset;
        scissor.push(matrix.peek().getPositionMatrix(), x - 6, y - 6, width + 10, height + 10);
        blur.render(ShapeProperties.create(matrix, x - 1, y - 0.5f, width + 2, height + 1).round(round).softness(0.5f).color(ColorUtil.getColor(16, 16, 16, 180)).build());

        if (showModuleNotification && moduleAnimation.getOutput().floatValue() > 0.01f) {
            float alpha = moduleAnimation.getOutput().floatValue();
            Color dotColor = currentModuleNotification.contains("§a") ? new Color(55, 255, 55, (int) (255 * alpha)) : new Color(255, 55, 55, (int) (255 * alpha));

            font.drawString(matrix, currentModuleNotificationClean, x + padding + 12, y - (padding / 2f) + (font.getStringHeight(currentModuleNotificationClean) / 2f), ColorUtil.getColor(255, 255, 255, (int) (255 * alpha)));
            rectangle.render(ShapeProperties.create(matrix, x + padding, y + padding, height - padding * 2, height - padding * 2).round(4f).color(ColorUtil.getColor(dotColor.getRed(), dotColor.getGreen(), dotColor.getBlue(), dotColor.getAlpha())).build());

        } else if (!mediaNull && !isPvp && mediaAnimation.getOutput().floatValue() > 0.01f) {
            float animationAlpha = mediaAnimation.getOutput().floatValue();
            float coverSize = baseHeight - padding * 2;
            float coverY = y + padding;
            float coverX = x + padding;

            if (coverTexture != null) {
                Render2DUtilSafeDraw(context, coverX, coverY, coverSize, animationAlpha);
            } else {
                rectangle.render(ShapeProperties.create(matrix, coverX, coverY, coverSize, coverSize).round(4f).color(ColorUtil.multAlpha(ColorUtil.getColor(50, 50, 50, 140), animationAlpha)).build());
            }

            font.drawString(matrix, fullTrack, x + baseHeight, y - (padding / 2f) + (font.getStringHeight(fullTrack) / 2f), ColorUtil.multAlpha(ColorUtil.WHITE, animationAlpha));

        } else if (!isPvp && mediaAnimation.getOutput().floatValue() < 0.99f) {
            float defaultAlpha = 1f - mediaAnimation.getOutput().floatValue();
            rectangle.render(ShapeProperties.create(matrix, x + padding, y + padding, baseHeight - padding * 2, baseHeight - padding * 2).round(4f).color(ColorUtil.multAlpha(ColorUtil.getClientColor(), defaultAlpha)).build());
            font.drawString(matrix, name, x + baseHeight, y - (padding / 2f) + (font.getStringHeight(name) / 2f), ColorUtil.multAlpha(ColorUtil.WHITE, defaultAlpha));

        } else if (isPvp && pvpAnimation.getOutput().floatValue() > 0.01f) {
            float pvpAlpha = pvpAnimation.getOutput().floatValue();
            rectangle.render(ShapeProperties.create(matrix, x + padding, y + padding, baseHeight + 2f - padding * 2, baseHeight - padding * 2).round(4f).color(ColorUtil.multAlpha(ColorUtil.RED, pvpAlpha)).build());

            FontRenderer timerFont = Fonts.getSize(13, Fonts.Type.DEFAULT);
            timerFont.drawString(matrix, pvpTimer, x + baseHeight - timerFont.getStringWidth(pvpTimer) / 2 - padding * 3.5f, y - (padding / 2f) + (timerFont.getStringHeight(pvpTimer) / 1.5f), ColorUtil.multAlpha(ColorUtil.WHITE, pvpAlpha));
            font.drawString(matrix, pvp, x + baseHeight + padding, y - (padding / 2f) + (font.getStringHeight(pvp) / 2f), ColorUtil.multAlpha(ColorUtil.WHITE, pvpAlpha));
        }

        scissor.pop();

        String time = LocalTime.now().format(DateTimeFormatter.ofPattern("HH:mm"));
        FontRenderer timeFont = Fonts.getSize(13, Fonts.Type.DEFAULT);
        timeFont.drawString(matrix, time, x - 1 - (padding * 3f) - timeFont.getStringWidth(time), y - 0.5f - (padding / 2f) + (timeFont.getStringHeight(time) / 2f), ColorUtil.WHITE);

        float baseBarY = y + padding + (Fonts.getSize(7, Fonts.Type.DEFAULT).getStringHeight("P") / 2f) - 4 + 1;
        float[] barYs = new float[3];
        for (int i = 0; i < 3; i++) barYs[i] = baseBarY + (10 - currentBarHeights[i]) / 2f;

        boolean hasActiveMusic = !mediaNull && !isPvp;
        float internetAlpha = internetAnimation.getOutput().floatValue();
        float barAlpha = barAnimation.getOutput().floatValue();

        if (hasActiveMusic && barAlpha > 0.01f && internetAlpha > 0.01f) {
            float combinedAlpha = internetAlpha * barAlpha;
            rectangle.render(ShapeProperties.create(matrix, x + width + (padding * 3f), barYs[0], 3.5F, currentBarHeights[0]).round(1f).color(ColorUtil.multAlpha(ColorUtil.WHITE, combinedAlpha)).build());
            rectangle.render(ShapeProperties.create(matrix, x + width + (padding * 3f) + 4, barYs[1], 3.5F, currentBarHeights[1]).round(1f).color(ColorUtil.multAlpha(ColorUtil.WHITE, combinedAlpha)).build());
            rectangle.render(ShapeProperties.create(matrix, x + width + (padding * 3f) + 8, barYs[2], 3.5F, currentBarHeights[2]).round(1f).color(ColorUtil.multAlpha(ColorUtil.WHITE, combinedAlpha)).build());
        }
    }

    private void Render2DUtilSafeDraw(DrawContext context, float coverX, float coverY, float coverSize, float animationAlpha) {
        try {
            var tex = mc.getTextureManager().getTexture(coverTextureLocation);
            if (tex != null) {
                ru.noxen.common.util.render.Render2DUtil.drawTexture(context, coverTextureLocation, coverX, coverY, coverSize, 4f,
                        (int) coverSize, (int) coverSize, (int) coverSize,
                        ColorUtil.getRect(1),
                        ColorUtil.multAlpha(ColorUtil.WHITE, animationAlpha));
                return;
            }
        } catch (Exception ignored) {
        }
        rectangle.render(ShapeProperties.create(context.getMatrices(), coverX, coverY, coverSize, coverSize).round(4f).color(ColorUtil.multAlpha(ColorUtil.getColor(50, 50, 50, 140), animationAlpha)).build());
    }
}
