package com.lahusa.mcbc_eval;

import com.lahusa.mcbc_eval.util.BiomeDistribution;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keybinding.v1.KeyBindingHelper;
import net.fabricmc.fabric.api.client.rendering.v1.WorldRenderEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.gui.hud.ChatHud;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.client.option.KeyBinding;
import net.minecraft.client.util.InputUtil;
import net.minecraft.client.util.ScreenshotRecorder;
import net.minecraft.client.util.Window;
import net.minecraft.client.world.ClientWorld;
import net.minecraft.registry.RegistryKey;
import net.minecraft.text.MutableText;
import net.minecraft.text.Text;
import net.minecraft.util.Formatting;
import net.minecraft.util.Util;
import net.minecraft.world.biome.Biome;
import org.apache.xmlrpc.XmlRpcException;
import org.apache.xmlrpc.client.*;
import org.lwjgl.glfw.GLFW;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Objects;
import java.util.Optional;

@net.fabricmc.api.Environment(net.fabricmc.api.EnvType.CLIENT)
public class MCBCEvalClient implements ClientModInitializer {

    // RPC client for communicating with the Python CNN
    private static XmlRpcClient rpcClient;
    // Keybind for starting the evaluation process
    private static KeyBinding evalKey;
    // Current stage of evaluation
    private static EvaluationStage stage = EvaluationStage.Idle;
    // Frames rendered since framebuffer downsizing
    private static int frameCounter = 0;
    // Biome from the last evaluation position
    private static RegistryKey<Biome> biome;
    private static boolean wasFullScreen = false;
    private static int originalWindowWidth;
    private static int originalWindowHeight;

    @Override
    public void onInitializeClient() {
        // Initialize RPC client and config
        rpcClient = new XmlRpcClient();
        XmlRpcClientConfigImpl rpcClientConfig = new XmlRpcClientConfigImpl();

        // Set server URL to localhost:8000 in config
        try {
            rpcClientConfig.setServerURL(new URL("http://127.0.0.1:8000"));
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }

        // Apply config to RPC client
        rpcClient.setConfig(rpcClientConfig);

        // Keybind registration
        evalKey = KeyBindingHelper.registerKeyBinding(new KeyBinding(
                "key.mcbc_eval.eval", // The translation key of the keybinding's name
                InputUtil.Type.KEYSYM, // The type of the keybinding, KEYSYM for keyboard, MOUSE for mouse.
                GLFW.GLFW_KEY_O, // The keycode of the key
                "category.mcbc_eval.keybinds" // The translation key of the keybinding's category.
        ));

        // Increment frame counter when done rendering frame
        WorldRenderEvents.END.register(
                context -> {
                    if(stage == EvaluationStage.Downsized) ++frameCounter;
                }
        );

        // Tick end handler
        ClientTickEvents.END_CLIENT_TICK.register(client -> {
            // Handle input
            if (evalKey.isPressed() && stage == EvaluationStage.Idle) {

                // Shrink window
                Window window = client.getWindow();
                wasFullScreen = window.isFullscreen();
                if(wasFullScreen) {
                    window.toggleFullscreen();
                }
                else {
                    originalWindowWidth = window.getWidth();
                    originalWindowHeight = window.getHeight();
                }
                window.setWindowedSize(384, 216);

                // Clear chat
                client.inGameHud.getChatHud().clear(false);

                // Update state
                stage = EvaluationStage.Downsized;
                frameCounter = 0;
            }
            // Downsized frame has been rendered in the meantime
            if(stage == EvaluationStage.Downsized && frameCounter > 1) {
                // Determine file path
                String filename = "eval-"+Util.getFormattedCurrentTime() + "-" + System.currentTimeMillis() % 1000 + ".png";
                String filepath = FabricLoader.getInstance().getGameDir().toFile() + "\\screenshots\\" + filename;

                // Get correct biome for prediction evaluation
                ClientWorld world = Objects.requireNonNull(client.world);
                ClientPlayerEntity player = Objects.requireNonNull(client.player);
                Optional<RegistryKey<Biome>> biomeKeyOpt = world.getBiome(player.getBlockPos()).getKey();
                biome = biomeKeyOpt.orElse(null);

                // Update state
                stage = EvaluationStage.Capturing;

                // Save screenshot and register completion callback
                ScreenshotRecorder.saveScreenshot(
                        FabricLoader.getInstance().getGameDir().toFile(),
                        filename,
                        client.getFramebuffer(),
                        (message) -> {
                            ChatHud chat = client.inGameHud.getChatHud();

                            // Print file name chat message
                            chat.addMessage(Text.literal("Screenshot Evaluation").formatted(Formatting.BOLD, Formatting.UNDERLINE));
                            chat.addMessage(Text.literal("\""+filename+"\"").formatted(Formatting.GRAY));
                            chat.addMessage(Text.literal(" "));

                            // Reset window to its original dimensions
                            Window window = client.getWindow();
                            if(wasFullScreen) {
                                window.toggleFullscreen();
                            }
                            else {
                                window.setWindowedSize(originalWindowWidth, originalWindowHeight);
                            }

                            // Make call to CNN Python RPC Server
                            try {
                                // Save call result
                                String result = (String) rpcClient.execute(
                                        "handle_image",
                                        new Object[] {
                                                 filepath
                                        }
                                );

                                // Write result to chat
                                boolean handledCorrectGroup = false;
                                String correctGroup = BiomeDistribution.getGroup(biome).toString().toLowerCase();
                                for(String line : result.split(" - ")) {
                                    MutableText text = Text.literal(line);
                                    if(!handledCorrectGroup) {
                                        String group = line.split(":")[0];
                                        if(group.equals(correctGroup)) {
                                            handledCorrectGroup = true;
                                            text = text.formatted(Formatting.GREEN, Formatting.BOLD);
                                        }
                                        else {
                                            text = text.formatted(Formatting.RED);
                                        }
                                    }
                                    chat.addMessage(text);
                                }
                            } catch (XmlRpcException e) {
                                throw new RuntimeException(e);
                            }

                            // Update state
                            stage = EvaluationStage.Idle;
                        }
                );
            }
        });
    }

    // Internal state enum
    private enum EvaluationStage {
        Idle,
        Downsized,
        Capturing
    }
}
