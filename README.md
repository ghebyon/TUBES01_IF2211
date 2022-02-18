# TUBES01_IF2211

# Algoritma Greedy
Algoritma greedy merupakan sebuah paradigma dalam penyelesaian persoalan secara bertahap. Solusi yang diberikan oleh algoritma greedy disusun step by step atau langkah demi langkah. Pada setiap langkah tersebut akan diambil keputusan yang paling optimal.

# Requirement
Untuk menjalankan bot, dibutuhkan beberapa aplikasi seperti Java JDK 8, IntelliJ IDEA, dan NodeJS.

# Build
File bot.java dan main.java terlebih dahulu dibuild sebagai file executable java (.jar) yang nantinya digunakan untuk menjalankan bot di game. Namun, sebelum menjalankan game terlebih dahulu dilakukan beberapa konfigurasi file pada :
- game-runner-config.json : file yang diberisikan konfigurasi lokasi file kedua pemain, hasil pertandingan, game engine yang digunakan, dan konfigurasi game yang digunakan.
- game-config.json : file yang berisikan konfigurasi game, seperti panjang lintasan, banyak lane, dll.
- bot.json : file yang berisikan identitas bot yang dibuat.

# Strategi

## Greedy Tweet
Algoritma greedy tweet ini merupakan salah satu strategi dalam penggunaan powerup tweet. Strategi yang diimplementasikan adalah dengan melihat kondisi dan posisi musuh sehingga penggunaan Tweet diharapkan akan selalu mengenai musuh. Pada setiap ronde, mobil akan melakukan pengecekan ketersediaan power up Tweet ini, jika tersedia maka akan digunakan berdasarkan kondisi musuh. Tweet akan digunakan ketika musuh sedang melakukan fix atau posisi musuh sudah jauh didepan atau tertinggal dibelakang. Tweet akan digunakan untuk memunculkan obstacle Cybertruck di depan musuh. Penempatan Cybertruck bervariasi tergantung kondisi musuh. Jika di depan musuh tidak terdapat obstacle maka akan langsung dimunculkan didepannya. Jika terdapat obstacle di depan musuh akan dimunculkan pada lane sebelah kiri atau kanan tergantung kondisi obstacle pada lane tersebut. Jika lane sebelah kiri musuh tidak terdapat obstacle dan lane sebelah kanan terdapat obstacle, maka akan dimunculkan pada lane sebelah kiri begitu juga untuk keadaan sebaliknya. Strategi ini digunakan dengan harapan musuh akan menabrak Cybertruck ketika sedang menghimdari obstacle.

## Greedy Turning Lane
Algoritma Greedy Turning Lane merupakan salah satu strategi dalam menentukan penggunaan perintah belok atau tetap di lane. Strategi yang diimplementasikan adalah dengan memeriksa lane pemain saat ini, lane sebelah kiri, dan lane sebelah kanan. Setiap ronde akan diperiksa apakah terdapat obstacle di depan pemain dan damage yang akan diterima pada lane yang menjadi kandidat untuk ditempati ronde selanjutnya. Algoritma lalu menentukan perintah optimal dengan membandingkan damage setiap lane. Lane yang akan dipilih adalah lane dengan damage minimal. Jika terdapat lane lebih dari 1 dengan damage minimal maka akan dipilih berdasarkan banyaknya powerup pada lane tersebut.

## Greedy Fix
Algoritma Greedy Fix merupakan salah satu strategi dalam menentukan penggunaan perintah fix untuk memperbaiki damage. Strategi yang diimplementasikan adalah dengan memeriksa damage mobil saat ini. Jika sudah lebih besar dari 3 maka akan langsung melakukan perintah fix. Jika damage sudah terlalu kecil akan diperiksa apakah mobil berada pada lane yang aman atau tidak untuk melakukan fix. Aman yang dimaksud adalah tidak dalam jangkauan emp musuh dan tidak ada obstacle pada block di depan pemain.

## Greedy Lizard
Greedy by Power Ups Lizard adalah strategi dalam melewati obstacle berupa wall, mud, dan/atau oil_spill. Pada setiap ronde, mobil akan melakukan pengecekan terhadap jalur yang ada dihadapannya sebanyak kecepatan mobil saat ini, serta melakukan pengecekan terhadap mobil musuh yang mungkin ada di depan dan dapat didahului. Jika pada range tersebut ada obstacle dan/atau mobil musuh dan mobil saat ini memiliki lizard maka mobil akan mengaktifkan power ups tersebut.

## Greedy EMP
Greedy by Power Ups EMP adalah strategi menembak mobil musuh yang berada di depan mobil saat ini menggunakan EMP. Untuk setiap ronde, jika pada lane saat ini tidak ada obstacle. Jika mobil musuh berada 2 block di depan dan tidak berada pada lane serta mobil saat ini memiliki EMP, maka EMP akan ditembakkan. Namun, terdapat kasus khusus ketika mobil saat ini berada pada lane yang sama, maka harus melakukan cek terlebih dahulu jika mobil saat ini tidak melebihi posisi musuh pada round selanjutnya. Jika sudah melakukan tembakan, maka pada round selanjutnya tidak melakukan tembakan lagi.

## Greedy Boost
Greedy by Power Ups Boost adalah strategi dalam menggunakan boost. Jika menggunakan skill ini maka kecepatan mobil akan menjadi 15. Pada setiap ronde, diperiksa apakah pada lane saat memiliki obstacle. Jika tidak memiliki obstacle, memiliki boost, dan boost counting­ sama dengan 0 maka akan digunakan boost.

## Greedy Oil
Greedy by Power Ups Oil adalah strategi untuk dalam menggunakan Oil. Oil akan meletakan Oil_Spill pada posisi kita saat ini untuk round selanjutnya. Jika musuh dan diri sendiri berada pada lane yang sama dan mobil musuh berada di belakang dengan jarak kurang dari 5 maka Oil akan digunakan. Jika lebih dari lima, maka akan diprediksi dengan memeriksi lane kanan-kiri, jika pada lane kanan-kiri ada obstacle maka kemungkinan musuh akan tetap pada lane saat ini.

# Author
13520026 Muhammad Fajar Ramadhan
13520079 Ghebyon Tohada Nainggolan
13520120 Afrizal Sebastian


