(() => {
      'use strict';
      const root = document.documentElement;
      const themeButton = document.getElementById('theme-toggle');
      function syncTheme() {
        const light = root.dataset.theme === 'light';
        themeButton.setAttribute('aria-label', light ? '切换到深色模式' : '切换到浅色模式');
        themeButton.setAttribute('aria-pressed', String(light));
        document.querySelector('meta[name="theme-color"]').content = light ? '#f9f9fc' : '#191a1e';
      }
      themeButton.hidden = false;
      syncTheme();
      themeButton.addEventListener('click', () => {
        root.dataset.theme = root.dataset.theme === 'dark' ? 'light' : 'dark';
        try { localStorage.setItem('jay-archive-theme', root.dataset.theme); } catch (_) {}
        syncTheme();
      });
      const menuButton = document.getElementById('menu-toggle');
      const nav = document.getElementById('nav-links');
      function closeMenu() { nav.dataset.open = 'false'; menuButton.setAttribute('aria-expanded', 'false'); menuButton.setAttribute('aria-label', '展开导航菜单'); }
      menuButton.addEventListener('click', () => {
        const open = nav.dataset.open !== 'true'; nav.dataset.open = String(open);
        menuButton.setAttribute('aria-expanded', String(open)); menuButton.setAttribute('aria-label', open ? '收起导航菜单' : '展开导航菜单');
      });
      nav.querySelectorAll('a').forEach(link => link.addEventListener('click', closeMenu));
      document.addEventListener('keydown', event => { if (event.key === 'Escape' && nav.dataset.open === 'true') { closeMenu(); menuButton.focus(); } });
      const styles = {
        miuix: { image:'assets/screenshots/style-miuix.jpg', label:'Miuix', title:'Miuix，熟悉而轻盈。', description:'以 Miuix 组件呈现圆角卡片和细腻的按压反馈。顶栏支持高斯与渐变模糊，配合可选液态玻璃，让浏览自然流动。', tags:['圆角卡片','按压反馈','顶部模糊可选'] },
        coui: { image:'assets/screenshots/style-coloros.jpg', label:'ColorOS · COUI', title:'ColorOS，柔和又有秩序。', description:'COUI 主题带来分组设置、圆形操作按钮与分体悬浮底栏。介绍页的分类栏固定在顶部，磨砂表面与渐变模糊自然衔接。关闭液态玻璃，即可体验 COUI 分体底栏。', tags:['固定介绍栏','统一磨砂材质','分体悬浮底栏'] },
        material: { image:'assets/screenshots/style-material3.jpg', label:'Material Design 3', title:'Material 3，简洁而自在。', description:'使用 AndroidX Material 3 原生组件，提供清晰的导航层级、卡片与设置控件。支持动态配色，让界面与手机壁纸的色彩呼应。', tags:['原生 Material 组件','动态配色','清晰的导航层级'] }
      };
      Object.values(styles).forEach(style => { const preload = new Image(); preload.src = style.image; });
      const tabs = [...document.querySelectorAll('.style-tab')];
      function selectStyle(tab, focus = false) {
        const key = tab.dataset.style, style = styles[key];
        tabs.forEach(item => { const active = item === tab; item.setAttribute('aria-selected', String(active)); item.tabIndex = active ? 0 : -1; });
        document.getElementById('style-name').textContent = style.title;
        document.getElementById('style-description').textContent = style.description;
        document.getElementById('style-tags').replaceChildren(...style.tags.map(text => { const tag = document.createElement('span'); tag.className = 'tag'; tag.textContent = text; return tag; }));
        const shot = document.getElementById('style-image');
        shot.src = style.image;
        shot.alt = style.label + ' 风格的关于页面';
        shot.parentElement.dataset.image = style.image;
        shot.parentElement.dataset.title = style.label + ' · 界面实拍';
        document.getElementById('style-caption').textContent = style.label + ' · 界面实拍';
        if (!matchMedia('(prefers-reduced-motion: reduce)').matches) {
          shot.animate([{opacity:.2,transform:'translateY(12px) scale(.98)'},{opacity:1,transform:'translateY(0) scale(1)'}], {duration:420,easing:'cubic-bezier(.2,.7,.2,1)'});
          document.querySelector('.style-content').animate([{opacity:.3,transform:'translateX(-8px)'},{opacity:1,transform:'translateX(0)'}], {duration:300,easing:'ease-out'});
        }
        document.getElementById('style-panel').setAttribute('aria-labelledby', tab.id);
        if (focus) tab.focus();
      }
      tabs.forEach((tab, index) => {
        tab.addEventListener('click', () => selectStyle(tab));
        tab.addEventListener('keydown', event => {
          let next;
          if (event.key === 'ArrowRight') next = (index + 1) % tabs.length;
          if (event.key === 'ArrowLeft') next = (index + tabs.length - 1) % tabs.length;
          if (event.key === 'Home') next = 0;
          if (event.key === 'End') next = tabs.length - 1;
          if (next !== undefined) { event.preventDefault(); selectStyle(tabs[next], true); }
        });
      });
      const dialog = document.getElementById('media-dialog'), image = document.getElementById('media-image'), video = document.getElementById('media-video');
      if (typeof dialog.showModal === 'function') {
        document.querySelectorAll('[data-image]').forEach(button => button.addEventListener('click', () => {
          image.src = button.dataset.image; image.alt = button.querySelector('img').alt; image.hidden = false; video.hidden = true;
          document.getElementById('media-title').textContent = button.dataset.title; dialog.showModal();
        }));
        const videoOpen = document.getElementById('video-open'); videoOpen.hidden = false;
        videoOpen.addEventListener('click', () => { image.hidden = true; video.hidden = false; document.getElementById('media-title').textContent = '杰作 · 启动动画'; dialog.showModal(); });
        document.getElementById('media-close').addEventListener('click', () => dialog.close());
        dialog.addEventListener('click', event => { if (event.target === dialog) { const box = dialog.getBoundingClientRect(); if (event.clientX < box.left || event.clientX > box.right || event.clientY < box.top || event.clientY > box.bottom) dialog.close(); } });
        dialog.addEventListener('close', () => video.pause());
      }
      if ('IntersectionObserver' in window) {
        const revealObserver = new IntersectionObserver(entries => {
          entries.forEach(entry => { if (entry.isIntersecting) { entry.target.classList.add('is-visible'); revealObserver.unobserve(entry.target); } });
        }, {threshold:.08});
        document.querySelectorAll('.feature, .section-head, .preview-card, .style-panel, .archive-grid, .faq-layout, .download').forEach((el, i) => {
          el.classList.add('reveal'); el.style.setProperty('--reveal-delay', (i % 4) * 65 + 'ms'); revealObserver.observe(el);
        });
        const links = [...nav.querySelectorAll('a')];
        const observer = new IntersectionObserver(entries => {
          entries.forEach(entry => { if (entry.isIntersecting) links.forEach(link => { if (link.hash === '#' + entry.target.id) link.setAttribute('aria-current', 'location'); else link.removeAttribute('aria-current'); }); });
        }, { rootMargin:'-15% 0px -55% 0px', threshold:0 });
        links.forEach(link => { const section = document.querySelector(link.hash); if (section) observer.observe(section); });
      }
    })();
