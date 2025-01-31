package net.runelite.client.plugins.ogPlugins.ogblastfurnace;


import net.runelite.api.*;
import net.runelite.client.plugins.microbot.Microbot;
import net.runelite.client.plugins.microbot.Script;
import net.runelite.api.coords.WorldPoint;
import net.runelite.client.plugins.microbot.util.bank.Rs2Bank;
import net.runelite.client.plugins.microbot.util.gameobject.Rs2GameObject;
import net.runelite.client.plugins.microbot.util.keyboard.VirtualKeyboard;
import net.runelite.client.plugins.microbot.util.inventory.Inventory;
import net.runelite.client.plugins.microbot.util.equipment.Rs2Equipment;
import net.runelite.client.plugins.microbot.util.math.Random;
import net.runelite.client.plugins.microbot.util.widget.Rs2Widget;
import net.runelite.client.plugins.ogPlugins.ogblastfurnace.enums.Bars;

import java.awt.event.KeyEvent;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;



public class ogBlastFurnaceScript extends Script {

    public static double version = 1.0;
    private int delayMin;
    private int delayMax;
    private int delayTarget;
    private int afkMax;
    private int afkTarget;
    private boolean useStamina;
    private boolean keepStaminaActive;
    private int staminaMin;
    private int staminaMax;
    private Bars barSelected;
    private boolean talkToForeman;
    private boolean useCoalBag;
    private boolean refillCoffer;
    private int refillCofferAmmount;
    private enum State{LOADING_ORE,RETRIEVING_ORE,BANKING,TO_MUCH_SUPPLIES,WAITING}
    private State botState;
    private List<Integer> blastFurnaceWorlds = new ArrayList<>(Arrays.asList(new Integer[]{355,356,357,358,386,381,395,424,466,494,495,496,515,516}));
    private final WorldPoint[] nextToBarDespensor = new WorldPoint[] { new WorldPoint(1939,4963,0), new WorldPoint(1940,4962,0)};

    private WorldPoint playerLocation(){return Microbot.getClient().getLocalPlayer().getWorldLocation();}
    private int getBFBars() {return Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getVarbitValue(barSelected.getBFBarID()));}
    private int getBFPrimaryOre() {return Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getVarbitValue(barSelected.getBFPrimaryOreID()));}
    private int getBFSecondaryOre() {return Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getVarbitValue(barSelected.getBFSecondaryOreID()));}
    private int getBFDispenserState() {return Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getVarbitValue(Varbits.BAR_DISPENSER));}
    private void iceGlovesEquip(){if(Rs2Equipment.hasEquipped(ItemID.ICE_GLOVES)){return;} Rs2Equipment.equipItemFast(ItemID.ICE_GLOVES); sleepUntil(() -> Rs2Equipment.hasEquipped(ItemID.ICE_GLOVES));}
    private void goldGlovesEquip(){if(Rs2Equipment.hasEquipped(ItemID.GOLDSMITH_GAUNTLETS)){return;} Rs2Equipment.equipItemFast(ItemID.GOLDSMITH_GAUNTLETS); sleepUntil(() -> Rs2Equipment.hasEquipped(ItemID.GOLDSMITH_GAUNTLETS));}
    private void openChest() {Rs2Bank.openCertainBank(Rs2GameObject.findObjectById(ObjectID.BANK_CHEST_26707));}
    private boolean playerAtRetrieveLocation() { return Arrays.asList(nextToBarDespensor).contains(playerLocation());}
    private int getRunEnergy(){ return Integer.parseInt(Rs2Widget.getWidget(10485788).getText());}
    private int getStamEffect() {return Microbot.getClientThread().runOnClientThread(() -> Microbot.getClient().getVarbitValue(Varbits.STAMINA_EFFECT));}
    private void stamPotUp() {
        Rs2Bank.withdrawItem(false,"Stamina potion(1)");
        sleepUntil(() -> Inventory.hasItem(ItemID.STAMINA_POTION1),2000);
        Inventory.useItemAction("Stamina potion(1)","drink");
    }
    public boolean run(ogBlastFurnaceConfig config) {
        mainScheduledFuture = scheduledExecutorService.scheduleWithFixedDelay(() -> {
            if (!super.run()) return;
            try {
                if(barSelected == null){setSettings(config);}
                calcState();
                if(botState == State.TO_MUCH_SUPPLIES) {depositOverflow();}
                else if(botState == State.BANKING){restock();}
                else if(botState == State.LOADING_ORE){loadConveyor();}
                else if(botState == State.RETRIEVING_ORE){retrieveBars();}

            } catch (Exception ex) {
                System.out.println(ex.getMessage());
            }
        }, 0, Random.random(0,120), TimeUnit.MILLISECONDS);
        return true;
    }
    private void calcState(){
        Microbot.status = "Calculating State";
        if(barSelected == Bars.SILVER_BAR || barSelected == Bars.GOLD_BAR){
            if(Inventory.isFull() && getBFPrimaryOre() > 20 && getBFBars() > 20){;botState = State.TO_MUCH_SUPPLIES;}
            else if(Inventory.hasItem(barSelected.getBarID()) || ((getBFBars() < 26 || getBFPrimaryOre() < 26) && !Inventory.hasItem(barSelected.getPrimaryOre()))){;botState = State.BANKING;}
            else if(getBFBars() >= 26 && getBFPrimaryOre() >= 26){botState = State.RETRIEVING_ORE;}
            else if(Inventory.hasItem(barSelected.getPrimaryOre())) {botState = State.LOADING_ORE;}
            else {botState = State.WAITING;}
        }
    }
    private void loadConveyor() {
        Microbot.status = "Loading Conveyor";
        if(barSelected == Bars.SILVER_BAR || barSelected == Bars.GOLD_BAR){
            if(Inventory.hasItem(barSelected.getPrimaryOre())){
                Rs2GameObject.interact(9100);
                sleepUntil(() -> !Inventory.hasItem(barSelected.getPrimaryOre()), 20000);
                callAFK(36,1000,3000);
                if(!Inventory.hasItem(barSelected.getPrimaryOre())){
                    walkToDispenser();
                }
            }
        }
    }
    private void walkToDispenser() {
        if(Random.random(1,5) == 3){
            iceGlovesEquip();
            sleep(120,200);
            Microbot.getWalker().walkCanvas(new WorldPoint(1940,4962,0));
            callAFK(36,1000,6183);
            sleepUntil(() -> playerAtRetrieveLocation());
            sleep(120,200);
        } else {
            Microbot.getWalker().walkCanvas(new WorldPoint(1940,4962,0));
            callAFK(38,1000,6258);
            sleepUntil(() -> playerAtRetrieveLocation());
            sleep(120,200);
            iceGlovesEquip();
            sleep(120,200);
        }
    }
    private void retrieveBars() {
        Microbot.status = "Retrieving Bars";
        sleep(140,170);
        iceGlovesEquip();
        if(getBFDispenserState() == 1){sleepUntil(() -> getBFDispenserState() == 2 || getBFDispenserState() == 3);}
        Rs2GameObject.interact(9092);
        sleepUntil(() -> Rs2Widget.findWidget("How many would you like to take?") != null);
        VirtualKeyboard.keyPress(KeyEvent.VK_SPACE);
        sleep(40,90);
        if(barSelected == Bars.GOLD_BAR){
            goldGlovesEquip();
            sleep(120,200);
        }
        sleepUntil(() -> Inventory.hasItem(barSelected.getBarID()));
    }
    private void restock() {
        openChest();
        callAFK(27,1000,6000);
        sleepUntil(() -> Rs2Bank.isOpen());
        if( Rs2Bank.isOpen()) {
            if(Inventory.hasItem(barSelected.getBarID())){
                //TODO Fix useItemAction
                //Inventory.useItemAction(barSelected.getBarID(), "deposit-all");
                Rs2Bank.depositAll(barSelected.getBarID());
            }
            Rs2Bank.scrollTo(Rs2Widget.findWidget(barName()));
            if( getStamEffect() <= 10|| getRunEnergy() <= 40){stamPotUp();}
            Microbot.getMouse().click(Rs2Widget.findWidgetExact(barName()).getBounds());
            sleepUntil(() -> Inventory.hasItem(ItemID.GOLD_ORE));
            sleep(50,80);
            VirtualKeyboard.keyPress(KeyEvent.VK_ESCAPE);
        }
    }
    private String barName() {
        switch (barSelected.getBarID()) {
            case (ItemID.GOLD_BAR):
                return "Gold Bar";
            case (ItemID.SILVER_BAR):
                return "Silver Bar";
        }
            return "";
    }
    private void depositOverflow() {
        openChest();
        sleepUntil(() -> Rs2Bank.isOpen());
        //if(Inventory.hasItem(barSelected.getBarID())){ Inventory.useItemAction(barSelected.getBarID(), new String[]{"deposit-all"});}
        Rs2Bank.depositAll(barSelected.getBarID());
        Rs2Bank.depositAll(barSelected.getPrimaryOre());
        sleep(50,80);
        VirtualKeyboard.keyPress(KeyEvent.VK_ESCAPE);

    }
    private void callAFK(int chance, int min, int max){
        Microbot.status = "Called an AFK";
        if(Random.random(1,chance) == 1){
            sleep(min,max);
        }
    }
    private void setSettings(ogBlastFurnaceConfig config){
        this.delayMin = config.delayMin();
        this.delayMax = config.delayMax();
        this.delayTarget = config.delayTarget();
        this.afkMax = config.afkMax();
        this.afkTarget = config.afkTarget();
        this.useStamina = config.useStamina();
        this.keepStaminaActive = config.keepStaminaActive();
        this.staminaMin = config.staminaMin();
        this.staminaMax = config.staminaMax();
        this.barSelected = config.getBars();
        this.talkToForeman = config.talkToForeman();
        this.useCoalBag = config.useCoalBag();
        this.refillCoffer = config.getRefill();
        this.refillCofferAmmount = config.getRefillAmount();
    }
}
