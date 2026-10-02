package vn.hoctoanai.mau.xau.application.port;

import vn.hoctoanai.mau.xau.infrastructure.client.MayNgoai;

/** Vi phạm: port của application phụ thuộc infrastructure. */
public interface CongNgoai {
    MayNgoai may();
}
