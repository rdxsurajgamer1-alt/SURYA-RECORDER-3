SURYA RECORDER – नया Floating Menu Source

ऐप में START दबाने पर छोटा Floating बटन आता है। उससे मेन्यू खुलेगा और Record दबाने पर ही Android screen-capture अनुमति के बाद रिकॉर्डिंग शुरू होगी।

मेन्यू: Ratio 9:16 / 16:9 / 1:1 / 4:3 / Custom width-height; Resolution 720p / 1080p / 2K / 4K; FPS 30 / 60 / 90; Audio Microphone / No Audio / Internal / Internal + Microphone.

जरूरी सीमाएँ: इस source में Microphone और No Audio रिकॉर्डिंग backend लागू हैं। Internal Audio और Internal + Microphone अभी केवल menu options हैं, वास्तविक capture लागू नहीं है। चुना हुआ ratio output dimensions बदलता है, स्क्रीन के किसी हिस्से को crop नहीं करता। 4K और 90 FPS फोन के encoder पर निर्भर हैं। वीडियो app-specific Movies/SURYA_RECORDER में सेव होता है।

यह source ZIP है, APK नहीं। Android Studio में SURYA_RECORDER folder खोलें, Gradle Sync करें और Build > Build APK(s) चलाएँ। इस वातावरण में Android Gradle build/फोन टेस्ट नहीं हुआ है।
