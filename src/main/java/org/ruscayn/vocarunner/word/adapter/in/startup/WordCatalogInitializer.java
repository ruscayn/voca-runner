package org.ruscayn.vocarunner.word.adapter.in.startup;

import lombok.RequiredArgsConstructor;
import org.ruscayn.vocarunner.word.application.port.in.LoadWordCatalogUseCase;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.stereotype.Component;

/** 서버가 요청을 받기 전에 적재한다 (ApplicationRunner · 준비 완료 이벤트는 서버가 열린 뒤라 첫 요청이 빈 목록을 만날 수 있음) */
@Component
@RequiredArgsConstructor
public class WordCatalogInitializer implements SmartInitializingSingleton {

    private final LoadWordCatalogUseCase loadWordCatalogUseCase;

    @Override
    public void afterSingletonsInstantiated() {
        loadWordCatalogUseCase.load();
    }

}
