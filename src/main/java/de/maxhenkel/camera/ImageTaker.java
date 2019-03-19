package de.maxhenkel.camera;

import de.maxhenkel.camera.net.MessagePartialImage;
import de.maxhenkel.camera.proxy.CommonProxy;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ScreenShotHelper;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import net.minecraftforge.fml.common.gameevent.TickEvent;
import net.minecraftforge.fml.relauncher.Side;
import net.minecraftforge.fml.relauncher.SideOnly;

import java.awt.image.BufferedImage;
import java.io.IOException;
import java.util.UUID;

@SideOnly(Side.CLIENT)
@Mod.EventBusSubscriber(modid = Main.MODID, value = Side.CLIENT)
public class ImageTaker {

    private static Minecraft mc = Minecraft.getMinecraft();
    private static boolean takeScreenshot;
    private static UUID uuid;
    private static boolean hide;

    public static void takeScreenshot(UUID id) {
        Minecraft mc = Minecraft.getMinecraft();

        hide = mc.gameSettings.hideGUI;
        mc.gameSettings.hideGUI = true;

        takeScreenshot = true;
        uuid = id;
        mc.displayGuiScreen(null);
    }

    @SubscribeEvent
    public static void onRenderTickEnd(TickEvent.RenderTickEvent event) {
        if (!event.phase.equals(TickEvent.Phase.END)) {
            return;
        }

        if (!takeScreenshot) {
            return;
        }

        BufferedImage image = ScreenShotHelper.createScreenshot(mc.displayWidth, mc.displayHeight, mc.getFramebuffer());

        mc.gameSettings.hideGUI = hide;
        takeScreenshot = false;

        new Thread(() -> sendScreenshot(image), "ProcessScreenshotThread").start();

    }

    private static void sendScreenshot(BufferedImage image) {
        if (image.getWidth() > 1080) {
            float ratio = ((float) image.getHeight()) / ((float) image.getWidth());
            int newHeight = ((int) (((float) 1080) * ratio));
            image = ImageTools.resize(image, 1080, newHeight);
        }

        byte[] data;
        try {
            data = ImageTools.toBytes(image);
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        int size = data.length;
        if (size < 30_000) {
            CommonProxy.simpleNetworkWrapper.sendToServer(new MessagePartialImage(uuid, 0, size, data));
        } else {

            int bufferProgress = 0;
            byte[] currentBuffer = new byte[30_000];
            for (int i = 0; i < size; i++) {
                if (bufferProgress >= currentBuffer.length) {
                    CommonProxy.simpleNetworkWrapper.sendToServer(new MessagePartialImage(uuid, i - currentBuffer.length, data.length, currentBuffer));
                    bufferProgress = 0;
                    currentBuffer = new byte[currentBuffer.length];
                }
                currentBuffer[bufferProgress] = data[i];
                bufferProgress++;
            }

            if (bufferProgress > 0) {
                byte[] rest = new byte[bufferProgress];
                System.arraycopy(currentBuffer, 0, rest, 0, bufferProgress);
                CommonProxy.simpleNetworkWrapper.sendToServer(new MessagePartialImage(uuid, size - rest.length, data.length, rest));
            }
        }
    }

}
