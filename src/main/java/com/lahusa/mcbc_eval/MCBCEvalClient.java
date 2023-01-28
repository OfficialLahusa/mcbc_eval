package com.lahusa.mcbc_eval;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.text.Text;
import net.minecraft.util.Util;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.*;
import org.apache.xmlrpc.common.XmlRpcHttpRequestConfigImpl;
import org.apache.xmlrpc.common.XmlRpcRequestProcessor;
import org.lwjgl.glfw.GLFW;

import java.net.MalformedURLException;
import java.net.URL;
import java.util.TimeZone;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class MCBCEvalClient implements ClientModInitializer {

    private static XmlRpcClient rpcClient;
    private static KeyBinding classificationKey;
    private static int stage = 0;
    private static int frameCounter = 0;

    @Override
    public void onInitializeClient() {
        rpcClient = new XmlRpcClient();
        XmlRpcClientConfigImpl rpcClientConfig = new XmlRpcClientConfigImpl();
        try {
            rpcClientConfig.setServerURL(new URL("http://127.0.0.1:8000"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
        rpcClient.setConfig(rpcClientConfig);

        // Keybind registration
        classificationKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcbc_datagen.classify", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_O, // The keycode of the key
                "category.mcbc_datagen.keybinds" // The translation key of the keybinding's category.
        ));

        WorldRenderEvents.END.register(
                context -> {
                    if(stage == 1) ++frameCounter;
                }
        );

        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            ChatHud chat = client.inGameHud.getChatHud();

            if (classificationKey.isPressed()) {
                if(stage == 0) {
                    client.getWindow().toggleFullscreen();
                    client.getWindow().setWindowedSize(384, 216);
                    chat.clear(false);
                    stage = 1;
                    frameCounter = 0;
                }

            }
            if(stage == 1 && frameCounter > 1) {
                String filename = "eval-"+Util.getFormattedCurrentTime() + "-" + System.currentTimeMillis()%1000 + ".png";
                String filepath = FabricLoader.getInstance().getGameDir().toFile().toString()+"\\screenshots\\"+filename;
                stage = 2;

                ScreenshotRecorder.saveScreenshot(
                        FabricLoader.getInstance().getGameDir().toFile(),
                        filename,
                        client.getFramebuffer(),
                        (message) -> {
                            chat.addMessage(Text.literal("Evaluating screenshot \""+filepath+"\""));
                            client.getWindow().toggleFullscreen();
                            try {
                                String result = (String) rpcClient.execute(
                                        "handle_image",
                                        new Object[] {
                                                 filepath
                                        }
                                );
                                chat.addMessage(Text.literal(result));
                            } catch (XmlRpcException e) {
                                throw new RuntimeException(e);
                            }
                            stage = 3;
                        }
                );
            }
            else if(stage == 3) {
                stage = 0;
            }
        });
    }
}
