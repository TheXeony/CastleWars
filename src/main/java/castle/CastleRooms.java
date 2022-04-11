package castle;

import arc.math.Mathf;
import arc.math.geom.Position;
import arc.struct.ObjectMap;
import arc.struct.Seq;
import arc.util.Interval;
import castle.ai.CastleAI;
import castle.components.Bundle;
import castle.components.CastleIcons;
import castle.components.PlayerData;
import mindustry.content.Blocks;
import mindustry.content.Fx;
import mindustry.entities.Units;
import mindustry.game.Team;
import mindustry.gen.Call;
import mindustry.gen.Groups;
import mindustry.gen.Iconc;
import mindustry.gen.Unit;
import mindustry.type.Item;
import mindustry.type.UnitType;
import mindustry.world.Block;
import mindustry.world.Tile;
import mindustry.world.blocks.storage.CoreBlock;

import static mindustry.Vars.tilesize;
import static mindustry.Vars.world;

public class CastleRooms {

    public static final Seq<Room> rooms = new Seq<>();
    public static final ObjectMap<Block, Integer> blockCosts = new ObjectMap<>();

    public static final int size = 8;
    public static Tile shardedSpawn, blueSpawn;

    public static void load() {
        blockCosts.putAll(
                Blocks.duo, 100,
                Blocks.scatter, 250,
                Blocks.scorch, 200,
                Blocks.hail, 450,
                Blocks.wave, 300,
                Blocks.lancer, 350,
                Blocks.arc, 150,
                Blocks.parallax, 500,
                Blocks.swarmer, 1250,
                Blocks.salvo, 500,
                Blocks.segment, 750,
                Blocks.tsunami, 850,
                Blocks.fuse, 1500,
                Blocks.ripple, 1500,
                Blocks.cyclone, 1750,
                Blocks.foreshadow, 4000,
                Blocks.spectre, 3000,
                Blocks.meltdown, 3000,

                Blocks.commandCenter, 750,
                Blocks.repairPoint, 300,
                Blocks.repairTurret, 1200
        );
    }

    public static class Room implements Position {
        public int x;
        public int y;

        public int startx;
        public int starty;
        public int endx;
        public int endy;

        public int cost;
        public int size;

        public float offset;
        public Tile tile;
        public String label = "";

        public Room(int x, int y, int cost, int size) {
            this.x = x;
            this.y = y;

            this.startx = x - size / 2;
            this.starty = y - size / 2;
            this.endx = x + size / 2 + size % 2;
            this.endy = y + size / 2 + size % 2;

            this.cost = cost;
            this.size = size;
            this.offset = size % 2 == 0 ? 0f : 4f;
            this.tile = world.tile(x, y);

            spawn();
            rooms.add(this);
        }

        public void update() {}

        public void buy(PlayerData data) {
            data.money -= cost;
        }

        public boolean canBuy(PlayerData data) {
            return data.money >= cost;
        }

        public boolean showLabel(PlayerData data) {
            return true;
        }

        public boolean check(float x, float y) {
            return x > startx * tilesize && y > starty * tilesize && x < endx * tilesize && y < endy * tilesize;
        }

        public float getX() {
            return x * tilesize + offset;
        }

        public float getY() {
            return y * tilesize + offset;
        }

        public void spawn() {
            boolean core = tile.block() == Blocks.coreNucleus;
            for (int x = startx; x <= endx; x++) for (int y = starty; y <= endy; y++) {
                Block floor = core || x == startx || y == starty || x == endx || y == endy ? Blocks.metalFloor5 : Blocks.metalFloor;
                world.tile(x, y).setFloor(floor.asFloor());
            }
        }
    }

    public static class BlockRoom extends Room {
        public Block block;
        public Team team;

        public boolean bought;

        public BlockRoom(Block block, Team team, int x, int y, int cost, int size) {
            super(x, y, cost, size);

            this.block = block;
            this.team = team;
            this.label = CastleIcons.get(block) + " :[white] " + cost;
        }

        public BlockRoom(Block block, Team team, int x, int y, int cost) {
            this(block, team, x, y, cost, block.size + 1);
        }

        /** Special for cores */
        public BlockRoom(Team team, int x, int y, int cost) {
            this(Blocks.coreNucleus, team, x, y, cost, Blocks.coreShard.size + 1);
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            bought = true;

            tile.setNet(block, team, 0);
            if (!(block instanceof CoreBlock)) tile.build.health(Float.MAX_VALUE);

            Groups.player.each(p -> Call.label(p.con, Bundle.format("events.buy", Bundle.findLocale(p), data.player.coloredName()), 3f, getX(), getY()));
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && showLabel(data);
        }

        @Override
        public boolean showLabel(PlayerData data) {
            return data.player.team() == team && !bought;
        }
    }

    public static class MinerRoom extends BlockRoom {
        public Item item;
        public Interval interval = new Interval();

        public MinerRoom(Item item, Team team, int x, int y, int cost) {
            super(Blocks.laserDrill, team, x, y, cost);

            this.item = item;
            this.label = "[" + CastleIcons.get(item) + "] " + CastleIcons.get(block) + " :[white] " + cost;
        }

        @Override
        public void update() {
            if (bought && interval.get(300f)) {
                Call.effect(Fx.mineHuge, getX(), getY(), 0f, team.color);
                Call.transferItemTo(null, item, 48, getX(), getY(), team.core());
            }
        }
    }

    public static class UnitRoom extends Room {

        public enum UnitRoomType {
            attack, defend
        }

        public UnitType unitType;
        public UnitRoomType roomType;

        public int income;

        public UnitRoom(UnitType unitType, UnitRoomType roomType, int income, int x, int y, int cost) {
            super(x, y, cost, 4);

            this.unitType = unitType;
            this.roomType = roomType;
            this.income = income;
            this.label = " ".repeat(Math.max(0, (String.valueOf(income).length() + String.valueOf(cost).length() + 2) / 2)) +
                    CastleIcons.get(unitType) + (roomType == UnitRoomType.attack ? " [accent]\uE865" : " [scarlet]\uE84D") +
                    "\n[gray]" + cost +
                    "\n[white]" + Iconc.blockPlastaniumCompressor + " : " + (income < 0 ? "[crimson]" : income > 0 ? "[lime]+" : "[gray]") + income;
        }

        @Override
        public void buy(PlayerData data) {
            super.buy(data);
            data.income += income;

            Tile tile;
            Unit unit;

            if (roomType == UnitRoomType.attack) {
                tile = data.player.team() == Team.sharded ? blueSpawn : shardedSpawn;
                unit = unitType.spawn(data.player.team(), tile.worldx() + Mathf.random(-40, 40), tile.worldy() + Mathf.random(-40, 40));
                unit.controller(new CastleAI());
            } else if (data.player.team().core() != null) {
                tile = data.player.team().core().tile;
                unit = unitType.spawn(data.player.team(), tile.worldx() + 40, tile.worldy() + Mathf.random(-40, 40));
                unit.controller(new CastleAI());
            }
        }

        @Override
        public boolean canBuy(PlayerData data) {
            return super.canBuy(data) && Units.getCap(data.player.team()) > data.player.team().data().unitCount;
        }

        @Override
        public boolean showLabel(PlayerData data) {
            return false;
        }
    }
}
