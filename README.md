# progressiveGIF
This project is aim to display gif when user download it from internet.

Thank to koral--/android-gif-drawable which i use it to decode LZW.

Notice that this is just a demo,it can only start and stop with GIF.

What i did in this project is just decode GIF in 1024(cache) bytes and display in a ImageView.

how to use:

GifManager manager = new GifManager.Builder().load(url).into(imageView).build();
manager.start();

url can be a http link which forward a GIF.
url can alse be a InputStream which forward a File.
imageView is android ImageView which you can display.

You can use manager.clear(); to collect memory.

reStart() or resume() is not deal with.
