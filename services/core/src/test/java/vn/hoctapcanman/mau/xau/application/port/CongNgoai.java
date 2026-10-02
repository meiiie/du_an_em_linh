package vn.hoctapcanman.mau.xau.application.port;

import vn.hoctapcanman.mau.xau.infrastructure.client.MayNgoai;

/** Vi phạm: port của application phụ thuộc infrastructure. */
public interface CongNgoai {
    MayNgoai may();
}
