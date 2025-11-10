package com.example.killBatch.itemStream;

public class ItemStreamExplain {

/**
 * ItemStream
 * 대부분의 청크지향처리 컴포넌트들이 ItemStream을 구현
 *
 * 역할
 * 1. 자원 초기화 및 해제
 * - Step의 시작과 끝에서 자원을 열고 닫음
 *      -> 어떻게? StepBuilder가 ItemStream 구현 여부 검사
 *      -> ItemStream 구현 시, open(), update(), close() 호출 목록 추가
 *
 * - open() - 파일 및 DB 커넥션을 맺음
 * - close() - 파일 핸들 및 DB 커넥션, 메모리 누수 방지
 *
 * 2. 메타데이터 관리 및 상태 추적
 * - open(ExecuteContext executeContext) - 이전 스텝까지 진행되어 저장된 실행 정보를 복원
 *      - 스텝이 재시작하는 경우, executeContext의 이전 스텝의 정보가 담겨 있음.
 *      - 해당 정보를 통해 재시작 지점을 찾아서 재시작 됨
 * - update(ExecuteContext executeContext) - 현재 작업이 어디까지 진행되었는지 저장
 *      - 스텝의 작업 트랜잭션이 커밋되기 직전, 메타데이터를 보관
 */

/**
 * Composite(위임) 상황에서 자원 관리
 * - 여러 컴포넌트를 순차적으로 실행시키는 Composite~, MultiResource~도 ItemStream을 구현
 * - 따라서 여러 컴포넌트들에 대해서 자원을 순차적으로 열고 닫으며 효율적 관리
 */

/**
 * ClassifierCompositeItemWriter
 * - ItemStream 구현 X
 */

/**
 * @StepScope 관련 유의사항
 *
 * 상황
 * @StepScope를 통해 설정, 구현은 MongoItemReader이지만, 반환타입을 ItemReader로 반환한 경우
 *
 * 동작
 * 1. JDK Proxy가 생성한 프록시 객체 등록
 * 2. 프록시 객체는 ItemStream을 구현하지 않음.
 * 3. 따라서 ItemStream 메서드가 호출되지 않음
 *
 * 결론
 * @StepScope를 적용할 때는 반드시 구체 클래스를 반환 타입으로 지정!
 */

}
