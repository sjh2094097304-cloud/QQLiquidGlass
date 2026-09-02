package io.github.liuran001.mmliquidglass;

import java.nio.file.Files;
import java.nio.file.Path;

public final class SourceSafetyTest {
    public static void main(String[] args) throws Exception {
        Path src = Path.of(args[0], "src/io/github/liuran001/mmliquidglass");
        String avatar = Files.readString(src.resolve("QqAvatarBridge.java"));
        String dock = Files.readString(src.resolve("QqSplitDock.java"));
        String installer = Files.readString(src.resolve("LiquidGlassInstaller.java"));
        require(!avatar.contains("source.draw(") && !avatar.contains("view.draw("), "no recursive native avatar draw");
        require(!dock.contains("badge.draw(") && dock.contains("glass.setQqBackdrop(backdrop)")
                && dock.contains("droplet.setQqBackdrop(backdrop)"), "shared QQ capture provider");
        String backdrop=Files.readString(src.resolve("QqGlassBackdrop.java"));
        require(backdrop.contains("source.getParent()!=parent") && backdrop.contains("source==pill")
                && backdrop.contains("capturing=false;dirty=false") && backdrop.contains("c.clipRect("), "sibling-only bounded capture with finally guard");
        require(!backdrop.contains("parent.draw(") && !backdrop.contains("getDecorView()"), "never capture the dock ancestor or window");
        require(!dock.contains("nativeBar.setLayoutParams") && !dock.contains("tab.setLayoutParams")
                && !dock.contains("nativeRow.setPadding") && !dock.contains("removeView(nativeBar)"), "native layout untouched");
        require(!avatar.contains("PixelCopy") && avatar.contains("new NativeImageReader()"), "no screen capture avatar fallback");
        String images=Files.readString(src.resolve("NativeImageReader.java"));
        require(!images.contains("view.draw(") && images.contains("clone==d") && images.contains("small.copy("), "detached image data only");
        String settings=Files.readString(src.resolve("QqSettingsEntry.java"));
        require(!settings.contains("removeView(list)") && !settings.contains("wrapper.addView(list"), "no settings RecyclerView reparenting");
        String nativeEntry=Files.readString(src.resolve("NativeSettingsBridge.java"));
        require(nativeEntry.contains("chain.proceed()") && nativeEntry.contains("new ArrayList<>(groups)"), "native settings data copy");
        String panel=Files.readString(src.resolve("QqSettingsPanel.java"));
        require(panel.contains("ContextThemeWrapper") && panel.contains("class Toggle extends View"), "explicit theme and visible switches");
        require(panel.contains("new DockOptions(GlassConfig.options)") && panel.contains("GlassConfig.save(activity,draft)"), "cancel-safe drafts");
        require(panel.contains("draft.preset(index); refreshControls();") && !panel.contains("draft.preset(index); select("), "preset updates in place");
        String preview=Files.readString(src.resolve("DockPreview.java"));
        require(preview.contains("new LiquidGlassPanel") && preview.contains("new DropletPanel") && preview.contains("new DropletDragController"), "preview uses production optics and springs");
        require(!preview.contains("GlassConfig.save") && !preview.contains("GlassConfig.options=") && !preview.contains("target.performClick")
                && preview.contains("density,false)") && preview.contains("removeCallbacks(applyTask)"), "isolated preview draft, input, diagnostics and lifecycle");
        require(panel.contains("preview.dispose()") && panel.contains("WindowInsets.Type.ime()"), "close cleanup and space for color keyboard");
        require(panel.contains("scroll.setClipToPadding(true)") && panel.contains("addAction(actions,export)")
                && panel.contains("outputParams.topMargin"), "feedback clipping and explicit spacing");
        String pause=dock.substring(dock.indexOf("private void pause()"),dock.indexOf("private void dispose()"));
        require(!pause.contains("setClipBounds(oldClip)") && !pause.contains("restoreChrome()")
                && !pause.contains("View.INVISIBLE"), "pause preserves outgoing frame");
        require(dock.contains("return target.performLongClick()") && !dock.contains("QqSettingsEntry.show(activity)"), "left long press belongs to QQ");
        require(dock.contains("DockOptions.Key.THEME_ICONS") && dock.contains("findThemeIcon(target,0)")
                && dock.contains("refreshThemeBitmap()") && dock.contains("themeBitmap"), "QQ theme icon capture/fallback path");
        require(panel.contains("toggle(c,THEME_ICONS)") && panel.contains("previewThemeIcons()"), "theme icon switch and preview snapshot");
        String export=Files.readString(src.resolve("FeedbackExport.java"));
        require(export.contains("ACTION_CREATE_DOCUMENT") && !export.contains("http://") && !export.contains("https://"), "user-selected local export only");
        require(dock.contains("restoreChrome()") && dock.contains("avatarBridge.dispose()"), "restore native chrome and dispose avatar work");
        require(avatar.contains("action.claimAction(token)") && avatar.contains("!excluded(target)"), "guard native action and reject module targets");
        require(installer.contains("QqSplitDock.scheduleInstall(activity);\n            return;"), "QQ bypasses legacy capture installer");
        int start = dock.indexOf("OnPreDrawListener preDraw");
        int end = dock.indexOf("private final View.OnLayoutChangeListener", start);
        String callback = dock.substring(start, end);
        require(!callback.contains("setLayoutParams(") && !callback.contains("sync()") && !callback.contains(".draw("), "no pre-draw capture or layout");
        require(dock.contains("removeOnPreDrawListener") && dock.contains("removeCallbacks(poll)"), "lifecycle cleanup");
        System.out.println("PASS: QQ capture/layout/click/lifecycle source regression checks (not a device runtime test)");
    }
    private static void require(boolean ok, String message) { if (!ok) throw new AssertionError(message); }
}
