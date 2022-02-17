package za.co.entelect.challenge;

import za.co.entelect.challenge.command.*;
import za.co.entelect.challenge.entities.*;
import za.co.entelect.challenge.enums.*;

import java.util.*;
import static java.lang.Math.max;

public class Bot {

    /*ATRIBUT BOT*/
    private GameState gameState;
    //Change lane
    private final static Command LEFT = new ChangeLaneCommand(-1);
    private final static Command RIGHT = new ChangeLaneCommand(1);
    //PowerUps
    private final static Command ACCEL = new AccelerateCommand();
    private final static Command BOOST = new BoostCommand();
    private final static Command OIL = new OilCommand();
    private final static Command LIZARD = new LizardCommand();
    private final static Command EMP = new EmpCommand();
    //FIX CAR
    private final static Command FIX = new FixCommand();
    
    private Car opponent;
    private Car myCar;
    private List<Lane[]> map;
    
    //Atribut Tambahan
    public static Position opponentFixPosition; //Posisi opponent saat sedang memperbaiki mobilnya
    public static int countEMP; //Menghitung penggunaan EMP, diisi oleh nilai 1(baru saja digunakan) dan 0 sebaliknya
    
    //Constructor Bot
    public Bot(Random random,GameState gameState) {
        this.gameState = gameState;
        this.myCar = gameState.player;
        this.opponent = gameState.opponent;
        this.map = gameState.lanes;
    }
    
    //Strategi Utama
    public Command run() {
        //Inisiasi atribut pendukung
        if(gameState.currentRound == 1){
            countEMP = 0;
            opponentFixPosition = this.opponent.position;
        }
        
        //Strategi Memperbaiki Mobil
        if (this.myCar.damage >= 3){
            return FIX;
        }else if (this.myCar.damage == 2 || this.myCar.damage ==1){
            if (isSafePosition()) {
                return FIX;
            }
        }
        
        //Strategi Menggunakan Boost
        //Periksa myCar memiliki PowerUps Boost dan sedang tidak menggunakan boost,
        //kemudian lakukan prediksi apakah Boost layak dipakai
        if(havePowerUps(PowerUps.BOOST, this.myCar.powerups) && this.myCar.boostCounter == 0 && predictToUseBoost()){
            return BOOST;
        }
        
        List<Object> nextBlock = getBlocksInFront(this.myCar.position.lane, this.myCar.position.block, this.myCar.speed);
        
        //Periksa apakah terdapat obstacle pada Block yang dipilih
        if(isThereObstacle(nextBlock)){ 
            //Jika terdapat  obstacle pada nextBlock
            
            //Strategi Menggunakan Lizard dan Strategi Perpindahan Lane
            if (havePowerUps(PowerUps.LIZARD, this.myCar.powerups) && 
                (predictToUseLizardCauseTerrain(nextBlock) || predictToUseLizardCauseOpponent())){
                //Periksa apakah myCar memiliki PowerUps Lizard
                //kemudian lakukan prediksi apakah Boost layak dipakai
                return LIZARD;
            }else{
                //Jika tidak pilih arah yang effektif
                return getEffectiveDirection_Calculate();
            }
        }else if(!isThereObstacle(nextBlock)){
            //Jika tidak terdapat obstacle pada nextBlock

            //Strategi Memprediksi apakah musuh akan menggunakan EMP dan cara yang mungkin untuk menghindarinya
            if (havePowerUps(PowerUps.EMP, this.opponent.powerups) && 
                this.myCar.position.block > this.opponent.position.block &&
                this.myCar.position.block - this.opponent.position.block <= 2){
                return dodgeEMP();
            }

            //Strategi Penggunaan EMP dan Strategi untuk overtake  opponent
            if (this.opponent.position.block > this.myCar.position.block ){
                //Jika myCar memiliki PowerUps EMP,
                //lakukan pemeriksaan apakah EMP layak digunakan
                /*  Kondisi layak penggunaan  EMP : 
                    Posisi opponent berada pada 2 blok di depan kita dan masih di dalam map pada state yg sama.
                    Namun terdapat kasus khusus, jika opponent berada di lane yg sama maka harus dipenuhi kondisi :
                        posisi myCar di state selanjutnya tidak melebihi posisi opponent di state selanjutnya.
                 * */
                if (havePowerUps(PowerUps.EMP, this.myCar.powerups) && 
                    countEMP == 0 &&  
                    this.opponent.position.block - this.myCar.position.block > 2 &&
                    this.opponent.position.block - this.myCar.position.block <= 20){
                    if (this.opponent.position.lane == this.myCar.position.lane){
                        if (this.opponent.position.block > this.myCar.position.block + this.myCar.speed){
                            countEMP = 1;
                            return EMP;
                        }
                    }else{
                        countEMP = 1;
                        return EMP;
                    }
                }else{
                    //Strategi Menggunakan Lizard dan Strategi Perpindahan Lane
                    countEMP = 0;
                    if (havePowerUps(PowerUps.LIZARD, this.myCar.powerups) &&
                        (predictToUseLizardCauseTerrain(nextBlock) || predictToUseLizardCauseOpponent())){
                        return LIZARD;
                    }else{
                        return getEffectiveDirection_Calculate();
                    }
                }
            }

            //Strategi Penggunaan Tweet
            /*Terdiri dari strategi pemilihan :
             * 1. posisi didepan opponent saat melakukan Fix
             * 2. posisi saat opponent berada di map sebelumnya atau di map selanjutnya atau
             *    di map saat ini namun berada di belakang */
            Position tweetPosition1 = predictToUseTweet1(opponentFixPosition);
            Position tweetPosition2 = predictToUseTweet2();
            if(tweetPosition1.block != 0 && tweetPosition1.lane != 0){
                return new TweetCommand(tweetPosition1.lane, tweetPosition1.block +this.opponent.speed + 4);
            }else{
                opponentFixPosition = this.opponent.position;
            }
            if(tweetPosition2.block != 0 && tweetPosition2.lane != 0){
                return new TweetCommand(tweetPosition2.lane, tweetPosition2.block);
            }
            //Strategi Penggunaan OIL
            if (havePowerUps(PowerUps.OIL, this.myCar.powerups) && predictToUseOil()){
                return OIL;
            }
        }
        
        return ACCEL;

    }
    
    //Method dengan return boolean
    /*  hitung terlebih dahulu jumlah damage yang diberikan jika melewati lane tersebut tanpa menggunakan lizard
        periksa apakah damage myCar + damage >= 3
        atau
        block myCar + speed myCar > block myCar + blok lastObstacle
        dengan lastObstacle adalah obstalce terakhir di lane pada saat pemeriksaan lane
    */
    private boolean predictToUseLizardCauseTerrain(List<Object> nextBlock){
        int damage = calculatePossibleDamageFromChoosenWay(nextBlock);
        if (this.myCar.damage + damage >= 3 || (this.myCar.position.block + this.myCar.speed > this.myCar.position.block + idxLastObstacle(nextBlock, lastObstacleInFront(nextBlock)))){
            return true;
        }
        return false; 
    }

    //Method dengan return boolean
    /* periksa apakah mungkin melangkahi opponent dengan menggunakan lizard
       berada di posisi lane yang sama, dan
       posisi block myCar + speed myCar > block opponet + speed opponent
     */
    private boolean predictToUseLizardCauseOpponent(){
        if (this.opponent.position.lane == this.myCar.position.lane && 
            this.opponent.position.block > this.myCar.position.block && 
            this.myCar.position.block + this.myCar.speed > this.opponent.position.block + this.opponent.speed){
            return true;
        }
        return false;
    }

    //Method dengan return boolean
    /* opponent berada pada jarak 1 s.d 5 di belakang 
       periksa lane yang mungkin akan dilewati opponent, bila pada lane-lane tersebut belum ada obstacle, 
       kembalikan true
     */
    private boolean predictToUseOil(){
        if (this.opponent.position.lane == this.myCar.position.lane && this.opponent.position.block < this.myCar.position.block ){
            if (this.myCar.position.block - this.opponent.position.block <= 5){
                return true;
            }else{
                if(this.myCar.position.lane == 1){
                    List<Object> RightBlocks = new ArrayList<>();
                    RightBlocks = getBlocksInFront(this.myCar.position.lane+1, this.myCar.position.block, this.myCar.speed);
                    if (isThereObstacle(RightBlocks)){
                        return true;
                    }
                }else if (this.myCar.position.lane == 4){
                    List<Object> LeftBlocks = new ArrayList<>();
                    LeftBlocks = getBlocksInFront(this.myCar.position.lane-1, this.myCar.position.block, this.myCar.speed);
                    if (isThereObstacle(LeftBlocks)){
                        return true;
                    }
                }else{
                    List<Object> RightBlocks = new ArrayList<>();
                    List<Object> LeftBlocks = new ArrayList<>();
                    RightBlocks = getBlocksInFront(this.myCar.position.lane+1, this.myCar.position.block, this.myCar.speed);
                    LeftBlocks = getBlocksInFront(this.myCar.position.lane-1, this.myCar.position.block, this.myCar.speed);
                    if (isThereObstacle(RightBlocks) || isThereObstacle(LeftBlocks)){
                        return true;
                    }
                }
            }
        }
        return false;
    }

    // Method dengan return Position
    // Memeriksa apakah di suatu posisi, opponent sedang melakukan fix
    // Akan mengembalikan posisi opponent bila sedang melakukan fix
    private Position predictToUseTweet1(Position fixPosition){
        Position target = new Position();
        target.block = 0;
        target.lane = 0;
        if(this.opponent.position.lane == fixPosition.lane && this.opponent.position.block == fixPosition.block){
            if(havePowerUps(PowerUps.TWEET, this.myCar.powerups)){
                target.block = this.opponent.position.block;
                target.lane = this.opponent.position.lane;
            }else{
                target.block = 0;
                target.lane = 0;
            }
        }
        return target;
    }

    // Method dengan return Position
    /* Posisi yang akan dikembalikan :
     * Kasus 1 : opponent berada pada jarak 2 s.d 5 di belakang 
            periksa lane yang mungkin akan dilewati opponent, bila pada lane-lane tersebut belum ada obstacle, 
            posisi target menjadi di lane tersebut dan blocknya +1 dari posisi block opponent
     * Kasus 2 : opponent berada pada jarak > 5 di belakang atau berada pada jarak >20 di depan
            posisi target menjadi tepat di depan opponent
     */
    private Position predictToUseTweet2(){
        Position target = new Position();
        target.block = 0;
        target.lane = 0;
        if(havePowerUps(PowerUps.TWEET, this.myCar.powerups)){
            //Prediksi Target peletakan bila pada state tersebut opponent berada pada jangkauan myCar
            if (this.myCar.position.block - this.opponent.position.block <= 5 && this.myCar.position.block - this.opponent.position.block > 1){
                int i = 1;
                while(i <= 4 && i != this.myCar.position.lane){
                    List<Object> blocks = new ArrayList<>();
                    blocks = getBlocksInFront(i, this.opponent.position.block, this.opponent.speed);
                    if (!isThereObstacle(blocks)){
                        target.block = this.opponent.position.block + 1;
                        target.lane = this.opponent.position.lane;
                    }else{
                        if (i == 1){
                            List<Object> RightBlocks = new ArrayList<>();
                            RightBlocks = getBlocksInFront(i+1, this.opponent.position.block, this.opponent.speed-1);
                            if (!isThereObstacle(RightBlocks)){
                                target.lane = i+1;
                                target.block = this.opponent.position.block + 1;
                            }
                        }else if(i == 4){
                            List<Object> LeftBlocks = new ArrayList<>();
                            LeftBlocks = getBlocksInFront(i-1, this.opponent.position.block, this.opponent.speed-1);
                            if (!isThereObstacle(LeftBlocks)){
                                target.lane = i-1;
                                target.block = this.opponent.position.block + 1;
                            }
                        }else{
                            List<Object> LeftBlocks = new ArrayList<>();
                            List<Object> RightBlocks = new ArrayList<>();
                            LeftBlocks = getBlocksInFront(i-1, this.opponent.position.block, this.opponent.speed-1);
                            RightBlocks = getBlocksInFront(i+1, this.opponent.position.block, this.opponent.speed-1);
                            if (!isThereObstacle(LeftBlocks) && !isThereObstacle(RightBlocks)){
                                target.lane= i - 1; //Pukul rata letak target tweet di sebelah kiri
                                target.block = this.opponent.position.block + 1;
                            }else if (!isThereObstacle(LeftBlocks) && isThereObstacle(RightBlocks)){
                                target.lane= i - 1;
                                target.block = this.opponent.position.block + 1;
                            }else if (!isThereObstacle(LeftBlocks) && isThereObstacle(RightBlocks)){
                                target.lane= i + 1;
                                target.block = this.opponent.position.block + 1;
                            }

                        }
                    }
                    i += 1;
                }
            }else if(this.myCar.position.block - this.opponent.position.block > 5 ||
                    this.myCar.position.block - this.opponent.position.block < 0) {
                target.lane = this.opponent.position.lane;
                target.block = this.opponent.position.block + 1;
            }
        }
        return target;
    }

    //Method dengan return boolean
    private boolean predictToUseBoost(){
        /*Method digunakan untuk mengecek apakah 15 blok di depan myCar 
          tidak memiliki jenis terrain yang merupakan obstacle*/
        List<Object> blocks = new ArrayList<>();
        blocks = getBlocksInFront(this.myCar.position.lane, this.myCar.position.block, 15);
        if (!isThereObstacle(blocks)){
            return true;
        }
        return false;
    }

    // Methode dengan return integer
    // Mengembalikan banyak PowerUps yang akan diterima bila melewati kumpulan blocks yang dipilih
    private int countPowerUpsFromChoosenWay(List<Object> blocks){
        int count = 0;
        for(int i = 0; i < blocks.size(); i++){
            if (blocks.get(i) == Terrain.OIL_POWER  ||
                    blocks.get(i) == Terrain.BOOST      ||
                    blocks.get(i) == Terrain.EMP        ||
                    blocks.get(i) == Terrain.LIZARD     ||
                    blocks.get(i) == Terrain.TWEET){
                count += 1;
            }
        }
        return count;
    }

    // Methode dengan return integer
    // Mengembalikan damage yang akan diterima bila melewati kumpulan blocks yang dipilih
    private int calculatePossibleDamageFromChoosenWay(List<Object> blocks){
        int countWall = 0, countMud = 0, countOilSpill = 0;
        for(int i = 0; i < blocks.size() ; i++){
            if (blocks.get(i) == Terrain.WALL ){
                countWall += 1;
            }else if (blocks.get(i) == Terrain.MUD){
                countMud += 1;
            }else if (blocks.get(i) == Terrain.OIL_SPILL){
                countOilSpill += 1;
            }
        }
        return countWall*2 + countMud*1 + countOilSpill*1;
    }

    //Method dengan return Command
    /* Pemeriksaan dilakukan pada lane disekitar posisi myCar 
            lane 1 -> periksa lane 1(current) 2(right)
            lane 2 -> periksa lane 1(left) 2(current) 3(right) ... dst;
     * Pemeriksaan di tiap lane dilakukan pada block yang akan dilewati 
            current lane -> periksa blok current block + 1 s.d. current block + current speed)
            left lane & left right->  periksa block current blokck s.d. current block + current speed - 1.
     * Lane yang dipilih adalah lane yang memiliki damage minimal
     * Jika terdapat > 1 lane yang memiliki damage minimal, pilih lane yang memiliki powerUps maksimal
     */
    public Command getEffectiveDirection_Calculate(){
        String direction = "Stay";
        List<Object> CurrentBlocks = getBlocksInFront(this.myCar.position.lane , this.myCar.position.block, this.myCar.speed);

        int damage_L = 0, damage_R = 0;
        int damage_C = calculatePossibleDamageFromChoosenWay(CurrentBlocks);
        int collectedPowerUps_C = countPowerUpsFromChoosenWay(CurrentBlocks);

        if (myCar.position.lane == 1){
            List<Object> RightBlocks = getBlocksInFront(this.myCar.position.lane + 1, this.myCar.position.block-1, this.myCar.speed);
            damage_R = calculatePossibleDamageFromChoosenWay(RightBlocks);
            if (damage_C < damage_R){
                direction = "Stay";
            }else if (damage_C > damage_R){
                direction = "Right";
            }else{
                int collectedPowerUps_R = countPowerUpsFromChoosenWay(RightBlocks);
                if (collectedPowerUps_C >= collectedPowerUps_R){
                    direction = "Stay";
                }else{
                    direction = "Right";
                }
            }
        }else if(myCar.position.lane == 4){
            List<Object> LeftBlocks = getBlocksInFront(this.myCar.position.lane - 1, this.myCar.position.block-1, this.myCar.speed);
            damage_L = calculatePossibleDamageFromChoosenWay(LeftBlocks);
            if (damage_C < damage_L){
                direction = "Stay";
            }else if (damage_C > damage_L){
                direction = "Left";
            }else{
                int collectedPowerUps_L = countPowerUpsFromChoosenWay(LeftBlocks);
                if (collectedPowerUps_C >= collectedPowerUps_L){
                    direction = "Stay";
                }else{
                    direction = "Left";
                }
            }
        }else {
            List<Object> LeftBlocks = getBlocksInFront(this.myCar.position.lane - 1, this.myCar.position.block - 1, this.myCar.speed);
            List<Object> RightBlocks = getBlocksInFront(this.myCar.position.lane + 1, this.myCar.position.block - 1, this.myCar.speed);
            damage_L = calculatePossibleDamageFromChoosenWay(LeftBlocks);
            damage_R = calculatePossibleDamageFromChoosenWay(RightBlocks);
            if (damage_C < damage_R) {
                if (damage_C > damage_L) {
                    direction = "Left";
                } else if (damage_C < damage_L) {
                    direction = "Stay";
                } else {
                    int collectedPowerUps_L = countPowerUpsFromChoosenWay(LeftBlocks);
                    if (collectedPowerUps_C >= collectedPowerUps_L) {
                        direction = "Stay";
                    } else {
                        direction = "Left";
                    }
                }
            } else if (damage_C > damage_R) {
                if (damage_L < damage_R) {
                    direction = "Left";
                } else if (damage_L > damage_R) {
                    direction = "Right";
                } else {
                    int collectedPowerUps_L = countPowerUpsFromChoosenWay(LeftBlocks);
                    int collectedPowerUps_R = countPowerUpsFromChoosenWay(RightBlocks);
                    if (collectedPowerUps_L > collectedPowerUps_R) {
                        direction = "Left";
                    } else if (collectedPowerUps_L < collectedPowerUps_R) {
                        direction = "Right";
                    } else {
                        //Random, tapi untuk sementara pilih left
                        direction = "Left";
                    }
                }
            } else {
                if (damage_C > damage_L) {
                    direction = "Left";
                } else if (damage_C <= damage_L) {
                    direction = "Stay";
                }
            }
        }

        if(direction == "Stay"){
            return ACCEL;
        }else if(direction == "Left"){
            return LEFT;
        }else if(direction == "Right"){
            return RIGHT;
        }
        return ACCEL;
    }

    // Method dengan return boolean
    // Memeriksa apakah pada suatu List PowerUps memiliki PowerUps yang diinginkan
    private Boolean havePowerUps(PowerUps name, PowerUps[] inCar){
        if(inCar != null){
            for(PowerUps pu: inCar){
                if(pu.equals(name)){
                    return true;
                }
            }
        }

        return false;
    }


    private Object lastObstacleInFront(List<Object> lane){
        Object lastObs = Terrain.EMPTY;

        int i = lane.size()-1;
        boolean found = false;
        while(i >= 0 && !found){
            if(lane.get(i) == Terrain.WALL || lane.get(i) == Terrain.OIL_SPILL || lane.get(i) == Terrain.MUD){
                lastObs = lane.get(i);
                found = true;
            }else{
                i--;
            }
        }
        return lastObs;
    }

    private int idxLastObstacle(List<Object> lane, Object nameTerrain){
        int idx = lane.size()-1;
        while(idx >= 0){
            if(lane.get(idx).equals(nameTerrain)){
                break;
            }else{
                idx--;
            }
        }
        return idx + 1;
    }

    //Method dengan return boolean
    private boolean isThereObstacle(List<Object> blocks){
        /*Method digunakan untuk memeriksa apakah pada blocks yang sebelumnya dipilih
          memiliki salah satu jenis terrain berikut : WALL, MUD, OIL_SPILL*/
        for(int i = 0; i < blocks.size() ; i++){
            if (blocks.get(i) == Terrain.WALL || blocks.get(i) == Terrain.MUD || blocks.get(i) == Terrain.OIL_SPILL){
                return true;
            }
        }
        return false;
    }

    //Method dengan return boolean
    private boolean isSafePosition(){
        // Method ini digunakan untuk memeriksa apakah posisi myCar sekarang dalam posisi aman.
        /* Posisi aman berada pada :
         * Blok yang akan dilewati selanjutnya tidak memiliki bad terrain (oil spill, wall, mud) dan
         * (jarak opponent dan myCar > 20 dengan myCar berada lebih di depan            atau
         *  jarak opponent dan myCar antara 1 dan 20 serta opponent tidak memiliki EMP  atau
         *  jarak opponent dan myCar > 15 dengan opponent berada lebih di depan)
         */
        List<Object> blocks = new ArrayList<>();
        if (havePowerUps(PowerUps.BOOST,this.myCar.powerups)){
            blocks = getBlocksInFront(this.myCar.position.lane, this.myCar.position.block, 15);
        }else{
            blocks = getBlocksInFront(this.myCar.position.lane, this.myCar.position.block, 9);
        }
        if (!isThereObstacle(blocks)){
            if (this.myCar.position.block > this.opponent.position.block && this.myCar.position.block - this.opponent.position.block > 20){
                return true;
            }else if (this.myCar.position.block > this.opponent.position.block &&
                    this.myCar.position.block - this.opponent.position.block <= 20 &&
                    this.myCar.position.block - this.opponent.position.block >= 1 &&
                    !havePowerUps(PowerUps.EMP, this.opponent.powerups)){
                return true;
            }
            else if(this.opponent.position.block > this.myCar.position.block && this.opponent.position.block - this.myCar.position.block > 15){
                return true;
            }
        }
        return false;
    }

    //Method dengan return Command
    private Command dodgeEMP(){
        //Jika sudah diprediksi, periksa di lane mana opponent berada
        // Pindah ke lane yang aman :
        // 1. lane 1 atau lane 2, maka pindah ke kanan
        // 2. lane 3 atau lane 4, maka pindah ke kiri
        if(this.opponent.position.lane == 1 || this.opponent.position.lane == 2){
            return RIGHT;
        }else{
            return LEFT;
        }
    }
   
    // Method dengan return List<Object>
    // Mengambil block yang berada di depan posisi (lane,block) sebesar size
    private List<Object> getBlocksInFront(int lane, int block, int size){
        List<Lane[]> map = this.map;
        List<Object> blocks = new ArrayList<>();
        int startBlock = map.get(0)[0].position.block;
        Lane[] laneList = map.get(lane-1);

        for(int i = max(block-startBlock+1,0) ; i <= block-startBlock+ 1 + size; i++){
            if (laneList[i] == null || laneList[i].terrain == Terrain.FINISH) {
                break;
            }
            blocks.add(laneList[i].terrain);
        }
        return blocks;
    }
}