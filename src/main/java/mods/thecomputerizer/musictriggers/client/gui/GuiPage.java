package mods.thecomputerizer.musictriggers.client.gui;

import mods.thecomputerizer.musictriggers.client.Translate;
import mods.thecomputerizer.musictriggers.client.gui.instance.Instance;
import mods.thecomputerizer.theimpossiblelibrary.util.client.GuiUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.util.ResourceLocation;
import net.minecraft.util.text.TextFormatting;
import org.lwjgl.input.Mouse;

import javax.vecmath.Point2i;
import javax.vecmath.Point4i;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.BiConsumer;

public class GuiPage extends GuiSuperType {

    private final String id;
    private final List<Icon> icons;
    private final boolean canEdit;
    private ButtonSuperType toggleMode;
    private boolean deleteMode;
    private int scrollPos;
    private boolean canScrollDown;

    public GuiPage(GuiSuperType parent, GuiType type, Instance configInstance, String id, List<Icon> icons, boolean buttons) {
        super(parent, type, configInstance);
        this.id = id==null ? type.getId() : id;
        this.icons = icons;
        this.canEdit = buttons;
        this.deleteMode = false;
        this.scrollPos = 0;
        this.canScrollDown = false;
    }

    public String getID() {
        return this.id;
    }

    public void updateIcons(List<Icon> icons) {
        this.icons.clear();
        this.icons.addAll(icons);
    }

    @Override
    public void initGui() {
        super.initGui();
        if(this.canEdit) {
            String displayName = Translate.guiGeneric(false,"button",this.id + "_add");
            int width = this.fontRenderer.getStringWidth(displayName)+4;
            int left = 96;
            addTopButton(left, Translate.guiGeneric(false,"button",displayName), width, new ArrayList<>(),
                    (screen, button) -> Minecraft.getMinecraft().displayGuiScreen(
                            new GuiPopUp(this,GuiType.POPUP,this.getInstance(),this.id,true,4,
                            new ArrayList<>(this.icons))),
                    this);
            left+=(width+16);
            displayName = Translate.guiGeneric(false,"button","delete_mode");
            width = this.fontRenderer.getStringWidth(displayName)+4;
            this.toggleMode = addTopButton(left, Translate.guiGeneric(false,"button",displayName), width,
                    Translate.guiNumberedList(3,"button","delete_mode","desc"),
                    (screen, button) -> toggleDeleteMode(!this.deleteMode),this);
        }
    }

    private void toggleDeleteMode(boolean isActive) {
        this.deleteMode = isActive;
        this.toggleMode.updateDisplayFormat(isActive ? TextFormatting.RED : null);
    }

    @Override
    public void handleMouseInput() throws IOException {
        super.handleMouseInput();
        int scroll = Mouse.getEventDWheel();
        if(scroll!=0) {
            if(scroll>1 && this.canScrollDown) {
                this.scrollPos++;
                this.canScrollDown = false;
            } else if(this.scrollPos>0) this.scrollPos--;
        }
    }

    @Override
    protected void mouseClicked(int mouseX, int mouseY, int mouseButton) {
        super.mouseClicked(mouseX, mouseY, mouseButton);
        if (mouseButton == 0) {
            Iterator<Icon> itr = this.icons.iterator();
            while (itr.hasNext()) {
                Icon icon = itr.next();
                if(this.deleteMode && icon.canDelete()) {
                    getInstance().deleteChannel(icon.channelName());
                    itr.remove();
                }
                else icon.onClick(this);
            }
        }
    }

    @Override
    protected void drawStuff(int mouseX, int mouseY, float partialTicks) {
        int top = this.spacing+24;
        drawIcons(mouseX,mouseY,top);
        drawLeftSide(top);
    }

    private void drawIcons(int mouseX, int mouseY, int top) {
        int left = this.width-(this.spacing*15);
        Point2i topLeft = new Point2i(left,top);
        boolean isLeft = true;
        for(Icon icon : this.icons) {
            if(isLeft) {
                topLeft.setX(left);
                topLeft.setY(top);
            }
            else topLeft.setX(left+(this.spacing*7));
            icon.drawIcon(topLeft,this.spacing,mouseX,mouseY,black(192),this.zLevel);
            if(!isLeft) top+=this.spacing*7;
            isLeft = !isLeft;
        }
    }

    private void drawLeftSide(int top) {
        int textHeight = this.fontRenderer.FONT_HEIGHT;
        int centerX = this.width/2;
        int left = this.spacing;
        int textX = left+(this.spacing/2);
        for(Icon icon : this.icons) {
            if(icon.getHover()) {
                GuiUtil.drawLine(new Point2i(left, top), new Point2i(centerX, top), white(128), 1f, this.zLevel);
                top += this.spacing;
                drawString(fontRenderer, icon.getDisplay(), textX, top, GuiUtil.WHITE);
                top += (textHeight + this.spacing);
                GuiUtil.drawLine(new Point2i(left, top), new Point2i(centerX, top), white(128), 1f, this.zLevel);
                top += this.spacing;
                top = GuiUtil.drawMultiLineString(this,icon.getDescription(),textX,centerX,top,textHeight+(this.spacing/2)
                )+(this.spacing/2);
                GuiUtil.drawLine(new Point2i(left, top), new Point2i(centerX, top), white(128), 1f, this.zLevel);
                break;
            }
        }
    }

    @Override
    protected void save() {

    }

    public static class Icon {

        private final ResourceLocation texture;
        private final ResourceLocation hoverTexture;
        private final String id;
        private final String display;
        private final String description;
        private final BiConsumer<GuiSuperType,String> handlerFunction;
        private final boolean separateDisplay;
        private final boolean canDelete;
        private boolean hover;
        public Icon(String id, ResourceLocation texture, ResourceLocation hoverTexture, boolean canDelete,
                    BiConsumer<GuiSuperType,String> onCLick) {
            this(id,null,texture,hoverTexture,canDelete,onCLick);
        }

        public Icon(String id, String displayID, ResourceLocation texture, ResourceLocation hoverTexture, boolean canDelete,
                    BiConsumer<GuiSuperType,String> onCLick) {
            this.texture = texture;
            this.hoverTexture = hoverTexture;
            this.id = id;
            if(!Objects.isNull(displayID)) {
                this.separateDisplay = true;
                Translate.guiGeneric(false,"titles",displayID,"name");
                this.display = Translate.guiGeneric(false,"titles",displayID,"name");
                this.description = Translate.guiGeneric(false,"titles",displayID,"desc");
            } else {
                this.separateDisplay = false;
                this.display = Translate.guiGeneric(false,"titles",id,"name");
                this.description = Translate.guiGeneric(false,"titles",id,"desc");
            }
            this.handlerFunction = onCLick;
            this.hover = false;
            this.canDelete = canDelete;
        }

        private boolean canDelete() {
            return this.canDelete;
        }

        /*
            This can only be done since the only case where an icon can be deleted is on the channel selection page
        */
        public String channelName() {
            return this.id;
        }

        private boolean isHovering(int mouseX, int mouseY, Point2i topLeft, int sideLength) {
            return mouseX>topLeft.x && mouseX<(topLeft.x+sideLength) && mouseY>topLeft.y && mouseY<(topLeft.y+sideLength);
        }

        public void drawIcon(Point2i topLeft, int spacing, int mouseX, int mouseY, Point4i color, float zLevel) {
            GuiUtil.drawBoxOutline(topLeft,spacing*6,spacing*6,new Point4i(255,255,255,192),
                    1f,zLevel);
            Point2i backgroundTopLeft = new Point2i(topLeft.x+(spacing/2),topLeft.y+(spacing/2));
            Point2i iconCenter = new Point2i((int)(backgroundTopLeft.x+(spacing*2.5f)),(int)(backgroundTopLeft.y+(spacing*2.5f)));
            this.hover = isHovering(mouseX,mouseY,backgroundTopLeft,spacing*5);
            if(hover) {
                GuiUtil.drawBox(backgroundTopLeft,spacing*5,spacing*5,GuiUtil.reverseColors(color),zLevel);
                GuiUtil.bufferSquareTexture(iconCenter,spacing*1.5f,this.hoverTexture);
            } else {
                GuiUtil.drawBox(backgroundTopLeft, spacing * 5, spacing * 5, color, zLevel);
                GuiUtil.bufferSquareTexture(iconCenter, spacing * 1.5f, this.texture);
            }
        }

        public void onClick(GuiSuperType parent) {
            if(this.hover) this.handlerFunction.accept(parent,this.id);
        }

        public boolean getHover() {
            return this.hover;
        }

        public String getDisplay() {
            if(this.separateDisplay) return this.display+" - "+this.id;
            return this.display;
        }

        public String getDescription() {
            return this.description;
        }
    }
}
