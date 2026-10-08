# -*- coding: utf-8 -*-
"""
Minecraft 像素贴图转换器
把任意图片批处理成 N x N（默认 32x32）MC 风格透明 PNG 贴图。
依赖: pillow, numpy  (pip install pillow numpy)
运行: python texture_converter.py
"""
import os
import threading
import queue
import traceback
import tkinter as tk
from tkinter import ttk, filedialog, messagebox

import numpy as np
from PIL import Image

APP_TITLE = "MC 像素贴图转换器  ·  32x32 Texture Tool"


# ------------------------------- 核心转换逻辑 -------------------------------
def convert_image(src, dst, size=32, colors=24, white_thresh=242, margin=1,
                  dither=False, bg_mode="white"):
    """
    bg_mode:
      "white"  白底抠透明
      "none"   不抠底，直接缩放（保留背景）
      "checker" 近似（暂未用）
    """
    im = Image.open(src).convert("RGBA")
    arr = np.array(im)

    if bg_mode == "white":
        minc = arr[..., :3].min(axis=-1)
        arr[minc > white_thresh, 3] = 0

    im = Image.fromarray(arr, "RGBA")
    bbox = im.getbbox()
    if bbox:
        im = im.crop(bbox)

    sw, sh = im.size
    scale = (size - margin * 2) / max(sw, sh)
    nw, nh = max(1, round(sw * scale)), max(1, round(sh * scale))
    im2 = im.resize((nw, nh), Image.LANCZOS)

    canvas = Image.new("RGBA", (size, size), (0, 0, 0, 0))
    canvas.paste(im2, ((size - nw) // 2, (size - nh) // 2), im2)

    rgb = canvas.convert("RGB")
    q = rgb.quantize(
        colors=max(2, colors),
        method=Image.MEDIANCUT,
        dither=Image.FLOYDSTEINBERG if dither else Image.NONE,
    )
    qrgb = q.convert("RGB")

    if bg_mode == "white":
        alpha = canvas.split()[3].point(lambda a: 255 if a > 40 else 0)
    else:
        alpha = canvas.split()[3]
    out = Image.merge("RGBA", (*qrgb.split(), alpha))
    out.save(dst)
    return out


# ------------------------------- GUI -------------------------------
class App(tk.Tk):
    def __init__(self):
        super().__init__()
        self.title(APP_TITLE)
        self.geometry("760x560")
        self.minsize(680, 520)

        self.files = []           # 待处理文件绝对路径
        self.msg_q = queue.Queue()
        self.out_dir = tk.StringVar(value=os.path.join(os.getcwd(), "output"))

        # 设置变量
        self.size_var = tk.IntVar(value=32)
        self.colors_var = tk.IntVar(value=24)
        self.thresh_var = tk.IntVar(value=242)
        self.margin_var = tk.IntVar(value=1)
        self.dither_var = tk.BooleanVar(value=False)
        self.bg_mode = tk.StringVar(value="white")

        self._build_ui()
        self.after(100, self._poll_queue)

    # ---------- UI 搭建 ----------
    def _build_ui(self):
        pad = {"padx": 8, "pady": 4}

        # 顶部：文件列表
        top = ttk.LabelFrame(self, text="待转换图片（可多选 PNG/JPG）")
        top.pack(fill="both", expand=True, **pad)

        self.listbox = tk.Listbox(top, selectmode=tk.EXTENDED, height=10)
        self.listbox.pack(side="left", fill="both", expand=True, padx=(8, 0), pady=6)
        sb = ttk.Scrollbar(top, command=self.listbox.yview)
        sb.pack(side="left", fill="y", pady=6)
        self.listbox.config(yscrollcommand=sb.set)

        btns = ttk.Frame(top)
        btns.pack(side="left", fill="y", padx=8, pady=6)
        ttk.Button(btns, text="添加文件", command=self.add_files).pack(fill="x", pady=2)
        ttk.Button(btns, text="添加文件夹", command=self.add_folder).pack(fill="x", pady=2)
        ttk.Button(btns, text="移除选中", command=self.remove_selected).pack(fill="x", pady=2)
        ttk.Button(btns, text="清空", command=lambda: self.listbox.delete(0, tk.END)).pack(fill="x", pady=2)

        # 中部：设置
        cfg = ttk.LabelFrame(self, text="参数")
        cfg.pack(fill="x", **pad)

        ttk.Label(cfg, text="贴图尺寸").grid(row=0, column=0, sticky="w", padx=6, pady=6)
        size_cb = ttk.Combobox(cfg, textvariable=self.size_var, width=6,
                               values=[16, 32, 48, 64, 128], state="readonly")
        size_cb.grid(row=0, column=1, padx=4)

        ttk.Label(cfg, text="色板颜色数").grid(row=0, column=2, sticky="w", padx=6)
        ttk.Spinbox(cfg, from_=4, to=64, textvariable=self.colors_var, width=6).grid(row=0, column=3)

        ttk.Label(cfg, text="白底阈值").grid(row=0, column=4, sticky="w", padx=6)
        ttk.Spinbox(cfg, from_=200, to=255, textvariable=self.thresh_var, width=6).grid(row=0, column=5)

        ttk.Label(cfg, text="边距(px)").grid(row=0, column=6, sticky="w", padx=6)
        ttk.Spinbox(cfg, from_=0, to=4, textvariable=self.margin_var, width=4).grid(row=0, column=7)

        ttk.Checkbutton(cfg, text="Floyd 抖动", variable=self.dither_var).grid(row=0, column=8, padx=8)

        ttk.Radiobutton(cfg, text="白底抠透明", variable=self.bg_mode, value="white").grid(row=1, column=0, columnspan=2, sticky="w", padx=6, pady=4)
        ttk.Radiobutton(cfg, text="保留背景", variable=self.bg_mode, value="none").grid(row=1, column=2, columnspan=3, sticky="w")

        # 输出目录
        out = ttk.Frame(self)
        out.pack(fill="x", **pad)
        ttk.Label(out, text="输出目录:").pack(side="left")
        ttk.Entry(out, textvariable=self.out_dir).pack(side="left", fill="x", expand=True, padx=6)
        ttk.Button(out, text="浏览…", command=self.pick_out).pack(side="left")

        # 底部：进度 + 日志 + 按钮
        bot = ttk.LabelFrame(self, text="日志")
        bot.pack(fill="both", expand=True, **pad)
        self.log = tk.Text(bot, height=8, state="disabled", bg="#1e1e1e", fg="#dcdcdc",
                           insertbackground="#dcdcdc")
        self.log.pack(fill="both", expand=True, padx=8, pady=(6, 0))

        bottom = ttk.Frame(self)
        bottom.pack(fill="x", **pad)
        self.progress = ttk.Progressbar(bottom, mode="determinate")
        self.progress.pack(side="left", fill="x", expand=True, padx=(0, 8))
        self.run_btn = ttk.Button(bottom, text="开始转换", command=self.start)
        self.run_btn.pack(side="right")

    # ---------- 列表操作 ----------
    def add_files(self):
        paths = filedialog.askopenfilenames(
            title="选择图片",
            filetypes=[("图片", "*.png *.jpg *.jpeg *.webp *.bmp"), ("所有文件", "*.*")])
        for p in paths:
            self._add_one(p)

    def add_folder(self):
        d = filedialog.askdirectory(title="选择文件夹")
        if not d:
            return
        for fn in sorted(os.listdir(d)):
            if fn.lower().endswith((".png", ".jpg", ".jpeg", ".webp", ".bmp")):
                self._add_one(os.path.join(d, fn))

    def _add_one(self, p):
        p = os.path.normpath(p)
        if p not in self.files:
            self.files.append(p)
            self.listbox.insert(tk.END, os.path.basename(p))

    def remove_selected(self):
        for i in reversed(self.listbox.curselection()):
            self.listbox.delete(i)
            del self.files[i]

    def pick_out(self):
        d = filedialog.askdirectory(title="选择输出目录")
        if d:
            self.out_dir.set(d)

    # ---------- 日志/队列 ----------
    def _log(self, msg):
        self.log.config(state="normal")
        self.log.insert(tk.END, msg + "\n")
        self.log.see(tk.END)
        self.log.config(state="disabled")

    def _poll_queue(self):
        try:
            while True:
                kind, payload = self.msg_q.get_nowait()
                if kind == "log":
                    self._log(payload)
                elif kind == "max":
                    self.progress.config(maximum=payload, value=0)
                elif kind == "step":
                    self.progress.step(payload)
                elif kind == "done":
                    self.run_btn.config(state="normal")
                    messagebox.showinfo("完成", payload)
                elif kind == "error":
                    self.run_btn.config(state="normal")
                    messagebox.showerror("出错了", payload)
        except queue.Empty:
            pass
        self.after(100, self._poll_queue)

    # ---------- 转换 ----------
    def start(self):
        if not self.files:
            messagebox.showwarning("提示", "请先添加图片")
            return
        os.makedirs(self.out_dir.get(), exist_ok=True)
        self.run_btn.config(state="disabled")
        threading.Thread(target=self._worker, daemon=True).start()

    def _worker(self):
        try:
            size = self.size_var.get()
            colors = self.colors_var.get()
            thresh = self.thresh_var.get()
            margin = self.margin_var.get()
            dither = self.dither_var.get()
            bg = self.bg_mode.get()
            outdir = self.out_dir.get()

            self.msg_q.put(("max", len(self.files)))
            ok, fail = 0, 0
            for i, src in enumerate(self.files):
                name = os.path.splitext(os.path.basename(src))[0]
                dst = os.path.join(outdir, f"{name}_{size}x{size}.png")
                self.msg_q.put(("log", f"[{i+1}/{len(self.files)}] {os.path.basename(src)}"))
                try:
                    convert_image(src, dst, size=size, colors=colors,
                                  white_thresh=thresh, margin=margin,
                                  dither=dither, bg_mode=bg)
                    self.msg_q.put(("log", f"    -> {dst}"))
                    ok += 1
                except Exception as e:
                    self.msg_q.put(("log", f"    !! 失败: {e}"))
                    fail += 1
                self.msg_q.put(("step", 1))
            self.msg_q.put(("done", f"全部完成：成功 {ok}，失败 {fail}\n输出目录：{outdir}"))
        except Exception:
            self.msg_q.put(("error", traceback.format_exc()))


if __name__ == "__main__":
    App().mainloop()
