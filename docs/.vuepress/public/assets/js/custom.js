/**
	Custom JS

	1. FIXED MENU
	2. MENU SMOOTH SCROLLING
	3. USERS TESTIMONIALS ( SLICK SLIDER )
	4. MOBILE MENU CLOSE

**/

(function( $ ){


	/* ----------------------------------------------------------- */
	/*  1. FIXED MENU
	/* ----------------------------------------------------------- */


		jQuery(window).bind('scroll', function () {
    		if ($(window).scrollTop() > 150) {

		        $('#mu-header').addClass('mu-fixed-nav');

			    } else {
			    $('#mu-header').removeClass('mu-fixed-nav');
			}
		});


	/* ----------------------------------------------------------- */
	/*  2. MENU SMOOTH SCROLLING
	/* ----------------------------------------------------------- */

		//MENU SCROLLING WITH ACTIVE ITEM SELECTED

		// Cache selectors
		var lastId,
		topMenu = $(".mu-menu"),
		topMenuHeight = topMenu.outerHeight()+13,
		// All list items
		menuItems = topMenu.find('a[href^=\\#]'),
		// Anchors corresponding to menu items
		scrollItems = menuItems.map(function(){
		  var item = $($(this).attr("href"));
		  if (item.length) { return item; }
		});

		// Bind click handler to menu items
		// so we can get a fancy scroll animation
		menuItems.click(function(e){
		  var href = $(this).attr("href"),
		      offsetTop = href === "#" ? 0 : $(href).offset().top-topMenuHeight+22;
		  jQuery('html, body').stop().animate({
		      scrollTop: offsetTop
		  }, 1500);
		  e.preventDefault();
		});

		// Bind to scroll
		jQuery(window).scroll(function(){
		   // Get container scroll position
		   var fromTop = $(this).scrollTop()+topMenuHeight;

		   // Get id of current scroll item
		   var cur = scrollItems.map(function(){
		     if ($(this).offset().top < fromTop)
		       return this;
		   });
		   // Get the id of the current element
		   cur = cur[cur.length-1];
		   var id = cur && cur.length ? cur[0].id : "";

		   if (lastId !== id) {
		       lastId = id;
		       // Set/remove active class
		       menuItems
		         .parent().removeClass("active")
		         .end().filter("[href=\\#"+id+"]").parent().addClass("active");
		   }
		});


	/* ----------------------------------------------------------- */
	/*  3. USERS TESTIMONIALS (SLICK SLIDER)
	/* ----------------------------------------------------------- */

		$('.mu-testimonial-slide').slick({
			arrows: false,
			dots: true,
			infinite: true,
			speed: 500,
			autoplay: true,
			cssEase: 'linear'
		});

	/* ----------------------------------------------------------- */
	/*  4. MOBILE MENU CLOSE
	/* ----------------------------------------------------------- */

		jQuery('.mu-menu').on('click', 'li a', function() {
		  $('.mu-navbar .in').collapse('hide');
		});


  /* ----------------------------------------------------------- */
  /*  5. Kestra
  /* ----------------------------------------------------------- */


  jQuery('#app').click(function () {
    console.log('sdfsd')
  });


})( jQuery );
