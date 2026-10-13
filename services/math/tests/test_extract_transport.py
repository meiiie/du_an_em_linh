"""PDF transport is bounded before parsing and stays inside the sandbox worker."""
import base64
import io
import types
import unittest
from unittest.mock import patch

from app.job_runner import _extract_pdf


class PdfTransportTest(unittest.TestCase):
    def read(self, payload):
        sources = []

        def reader(source):
            sources.append(source.read() if isinstance(source, io.BytesIO) else source)
            return types.SimpleNamespace(pages=[types.SimpleNamespace(extract_text=lambda: "Tài liệu lớp")])

        with patch.dict("sys.modules", {"pypdf": types.SimpleNamespace(PdfReader=reader)}):
            result = _extract_pdf(payload)
        return result, sources

    def test_base64_passes_exact_pdf_bytes_without_local_file(self):
        raw = b"%PDF-1.7\ntransport fixture"
        result, sources = self.read({"pdf_base64": base64.b64encode(raw).decode()})
        self.assertEqual(result, {"text": "Tài liệu lớp", "trang_thai": "OK"})
        self.assertEqual(sources, [raw])

    def test_existing_path_transport_is_preserved(self):
        result, sources = self.read({"path": "/tmp/a-document.pdf"})
        self.assertEqual(result["trang_thai"], "OK")
        self.assertEqual(sources, ["/tmp/a-document.pdf"])

    def test_invalid_encoding_never_reaches_pdf_parser(self):
        for invalid in ["%%%", 42, "", "UE RG"]:
            result, sources = self.read({"pdf_base64": invalid})
            self.assertEqual(result["trang_thai"], "KHONG_TRICH_DUOC")
            self.assertEqual(sources, [])

    def test_oversized_input_is_rejected_before_pdf_parser(self):
        value = base64.b64encode(b"x" * (5 * 1024 * 1024 + 1)).decode()
        result, sources = self.read({"pdf_base64": value})
        self.assertEqual(result["trang_thai"], "KHONG_TRICH_DUOC")
        self.assertEqual(result["ly_do"], "FILE_QUA_LON")
        self.assertEqual(sources, [])

    def test_binary_transport_does_not_fallback_to_path(self):
        result, sources = self.read({"pdf_base64": "%%%", "path": "/tmp/a-document.pdf"})
        self.assertEqual(result["trang_thai"], "KHONG_TRICH_DUOC")
        self.assertEqual(sources, [])


if __name__ == "__main__":
    unittest.main()
