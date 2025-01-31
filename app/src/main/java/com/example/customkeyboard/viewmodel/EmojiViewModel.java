package com.example.customkeyboard.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;

import com.example.customkeyboard.model.EmojiRepository;

public class EmojiViewModel extends ViewModel {
    private final EmojiRepository emojiRepository;
    private final MutableLiveData<String> suggestEmoji = new MutableLiveData<>();

    EmojiViewModel() {
        emojiRepository = new EmojiRepository();
    }

    public LiveData<String> getSuggestEmoji() {
        return suggestEmoji;
    }

    public void proceedEmojiSearch(String text) {
        String emoji = emojiRepository.getEmoji(text);
        suggestEmoji.setValue(emoji);
    }

}
