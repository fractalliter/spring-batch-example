package com.ecom.winners.notifiers;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.batch.core.listener.ChunkListener;
import org.springframework.batch.infrastructure.item.Chunk;

/**
 * Ghi log ranh giới từng chunk: mỗi khi Reader đọc đủ một xấp và Writer ghi
 * xong, listener in một dòng "── Chunk #N ──" kèm số item trong xấp, để log
 * dashboard chia thành các đoạn dễ đọc theo đúng cơ chế chunk processing.
 *
 * <p>Spring Batch 6 (ChunkOrientedStep) chỉ gọi các biến thể typed
 * {@code beforeChunk(Chunk<I>)} / {@code afterChunk(Chunk<O>)}, nên listener
 * override đúng những hàm đó. Bean nên khai báo {@code @StepScope} để bộ đếm
 * chunk được tạo lại từ đầu cho mỗi lần chạy step.</p>
 */
public class ChunkLogListener implements ChunkListener<Object, Object> {

    private static final Logger logger = LoggerFactory.getLogger(ChunkLogListener.class);

    private long chunkNumber = 0;

    @Override
    public void beforeChunk(Chunk<Object> chunk) {
        chunkNumber += 1;
        logger.info("── Chunk #{} bắt đầu ──", chunkNumber);
    }

    @Override
    public void afterChunk(Chunk<Object> chunk) {
        logger.info("── Chunk #{} ghi xong: {} mục (chunk này được commit giữ chắc)",
                chunkNumber, chunk.getItems().size());
    }

    @Override
    public void onChunkError(Exception error, Chunk<Object> chunk) {
        logger.info("── Chunk #{} lỗi ({}), rollback — item trong chunk bị bỏ lại", chunkNumber,
                error.getClass().getSimpleName());
    }
}
